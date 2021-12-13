/**
 * Copyright (C) 2016
 *   Michael Mosmann <michael@mosmann.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.flapdoodle.reverse;

import de.flapdoodle.checks.Preconditions;
import de.flapdoodle.graph.Graphs;
import de.flapdoodle.graph.Loop;
import de.flapdoodle.graph.VerticesAndEdges;
import org.immutables.builder.Builder;
import org.immutables.value.Value;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TransitionWalker {
	private static final String JAVA_LANG_PACKAGE = "java.lang.";

	private final DefaultDirectedGraph<Transitions.Vertex, DefaultEdge> graph;

	private TransitionWalker(DefaultDirectedGraph<Transitions.Vertex, DefaultEdge> graph) {
		this.graph = graph;
	}

	private static Map<StateID<?>, State<?>> resolve(List<Transition<?>> transitions, Set<StateID<?>> destinations,
		StateLookup stateOfType, List<Listener> initListener) {
		Map<StateID<?>, State<?>> ret = new LinkedHashMap<>();

		for (Transition<?> transition : transitions) {
			if (destinations.contains(transition.destination())) {
				State<?> newState = resolve(stateOfType, initListener, transition);
				ret.put(transition.destination(), newState);
			}
		}

		return ret;
	}
	private static <T> State<T> resolve(StateLookup stateOfType, List<Listener> initListener, Transition<T> transition) {
		State<T> state = transition.result(stateOfType.limitedTo(transition.sources()));
		initListener.forEach(listener -> listener.onStateReached(transition.destination(), state.value()));
		return state;
	}

	public <D> ReachedState<D> initState(StateID<D> destination, Listener... listener) {
		return initState(new LinkedHashMap<>(), destination, Collections.unmodifiableList(Arrays.asList(listener)));
	}

	public <D> Transition<D> asTransitionTo(TransitionWalker.TransitionMapping<D> mapping, Listener... listener) {
		Transitions.StateVertex destination = Transitions.StateVertex.of(mapping.destination().source());
		Preconditions.checkArgument(graph.containsVertex(destination), "state %s is not part of this init process", asMessage(mapping.destination().source()));

		Collection<VerticesAndEdges<Transitions.Vertex, DefaultEdge>> dependencies = dependenciesOf(graph, destination);
		Set<StateID<?>> sources = missingSources(dependencies, new LinkedHashMap<>());

		return ImmutableMappedWrapper.<D>builder()
			.walker(this)
			.transitionMapping(mapping)
			.addListener(listener)
			.addAllMissingSources(sources)
			.build();
	}

	@Value.Immutable
	public static abstract class TransitionMapping<D> {
		@Builder.Parameter
		public abstract StateMapping<D> destination();

		public abstract List<StateMapping<?>> mappings();

		protected  <T> Optional<StateID<T>> findDestinationOf(StateID<T> source) {
			return mappings().stream()
				.filter(it -> it.source().equals(source))
				.map(it -> (StateMapping<T>) it)
				.findFirst()
				.map(StateMapping::destination);
		}

		protected <T> StateID<T> destinationOf(StateID<T> source) {
			return findDestinationOf(source).orElse(source);
		}

		protected <T> Optional<StateID<T>> findSourceOf(StateID<T> destination) {
			return mappings().stream()
				.filter(it -> it.destination().equals(destination))
				.map(it -> (StateMapping<T>) it)
				.findFirst()
				.map(StateMapping::source);
		}

		protected <T> StateID<T> sourceOf(StateID<T> destination) {
			return findSourceOf(destination).orElse(destination);
		}

		public static <D> ImmutableTransitionMapping.Builder<D> builder(StateID<D> destination) {
			return builder(StateMapping.of(destination,destination));
		}

		public static <D> ImmutableTransitionMapping.Builder<D> builder(StateMapping<D> mapping) {
			return ImmutableTransitionMapping.builder(mapping);
		}
	}

	@Value.Immutable
	public static abstract class StateMapping<T> {
		@Value.Parameter
		public abstract StateID<T> source();
		@Value.Parameter
		public abstract StateID<T> destination();

		public boolean isDirect() {
			return source().equals(destination());
		}

		public static <T> StateMapping<T> of(StateID<T> source, StateID<T> destination) {
			return ImmutableStateMapping.of(source,destination);
		}
	}

	@Value.Immutable
	static abstract class MappedWrapper<T> implements Transition<T> {

		protected abstract TransitionWalker walker();
		protected abstract List<Listener> listener();
		protected abstract TransitionMapping<T> transitionMapping();
		protected abstract Set<StateID<?>> missingSources();

		@Value.Auxiliary
		DefaultDirectedGraph<Transitions.Vertex, ?> graph() {
			return walker().graph;
		}

		@Override
		@Value.Lazy
		public StateID<T> destination() {
			return transitionMapping().destination().destination();
		}

		@Override
		@Value.Lazy
		public Set<StateID<?>> sources() {
			return missingSources().stream()
				.map(transitionMapping()::sourceOf)
				.collect(Collectors.toSet());
		}

		@Override
		@Value.Auxiliary
		public State<T> result(StateLookup lookup) {
			Map<StateID<?>, State<?>> stateMap=sources().stream()
				.collect(Collectors.toMap(transitionMapping()::destinationOf, id -> State.of(lookup.of(id))));

			ReachedState<T> reachedState = walker().initState(stateMap, transitionMapping().destination().source(), listener());
			return State.of(reachedState.current(), ignore -> reachedState.close());
		}
	}

	private <D> ReachedState<D> initState(Map<StateID<?>, State<?>> currentStateMap, StateID<D> dest, List<Listener> initListener) {
		Preconditions.checkArgument(!currentStateMap.containsKey(dest), "state %s already initialized", asMessage(dest));

		Transitions.StateVertex destination = Transitions.StateVertex.of(dest);
		Preconditions.checkArgument(graph.containsVertex(destination), "state %s is not part of this init process", asMessage(dest));

		Map<StateID<?>, State<?>> stateMap = new LinkedHashMap<>(currentStateMap);
		List<Collection<NamedTypeAndState<?>>> initializedStates = new ArrayList<>();

		Collection<VerticesAndEdges<Transitions.Vertex, DefaultEdge>> dependencies = dependenciesOf(graph, destination);

		if (!dependencies.isEmpty()) {
			Set<StateID<?>> missingSources = missingSources(dependencies, currentStateMap);

			Preconditions.checkArgument(missingSources.isEmpty(), "missing transitions: %s", asMessage(missingSources));
		}

		for (VerticesAndEdges<Transitions.Vertex, DefaultEdge> set : dependencies) {
//			System.out.println("Set: "+set);
//			set.vertices().forEach(vertex -> {
//				System.out.println(" must init "+vertex);
//			});

			List<Transition<?>> transitions = set.vertices().stream()
				.filter(it -> it instanceof Transitions.TransitionVertex)
				.map(it -> (Transitions.TransitionVertex) it)
				.map(Transitions.TransitionVertex::transition)
				.collect(Collectors.toList());

			Set<StateID<?>> destinations = transitions.stream()
				.map(Transition::destination)
				.collect(Collectors.toSet());

			Set<StateID<?>> needInitialization = filterNotIn(stateMap.keySet(), destinations);

			try {
				Map<StateID<?>, State<?>> newStatesAsMap = resolve(transitions, needInitialization,
					new MapBasedStateLookup(stateMap), initListener);
				if (!newStatesAsMap.isEmpty()) {
					initializedStates.add(asNamedTypeAndState(newStatesAsMap));
					stateMap.putAll(newStatesAsMap);
				}
			}
			catch (RuntimeException ex) {
				tearDown(initializedStates, initListener);
				throw new RuntimeException("error on transition to " + asMessage(needInitialization) + ", rollback", ex);
			}
		}

		return new ReachedState<>(this, initializedStates, stateMap, stateOfMap(stateMap, dest), initListener);
	}

	private static Set<StateID<?>> missingSources(Collection<VerticesAndEdges<Transitions.Vertex, DefaultEdge>> dependencies,
		Map<StateID<?>, State<?>> currentStateMap) {
		return dependencies.stream()
			.findFirst()
			.map(VerticesAndEdges::vertices)
			.orElse(Collections.emptySet()).stream()
			.filter(it -> it instanceof Transitions.StateVertex)
			.map(it -> ((Transitions.StateVertex) it).stateId())
			.filter(it -> !currentStateMap.containsKey(it))
			.collect(Collectors.toSet());
	}

	private static Collection<VerticesAndEdges<Transitions.Vertex, DefaultEdge>> dependenciesOf(
		DefaultDirectedGraph<Transitions.Vertex, DefaultEdge> routesAsGraph, Transitions.StateVertex destination) {
		DefaultDirectedGraph<Transitions.Vertex, DefaultEdge> filtered = Graphs.filter(routesAsGraph,
			v -> v.equals(destination) || isDependencyOf(routesAsGraph, v, destination));
		Collection<VerticesAndEdges<Transitions.Vertex, DefaultEdge>> roots = Graphs.rootsOf(filtered);
		// System.out.println("dependencies -> ");
		// roots.forEach(System.out::println);
		return roots;
	}

	private static boolean isDependencyOf(DefaultDirectedGraph<Transitions.Vertex, DefaultEdge> routesAsGraph, Transitions.Vertex source,
		Transitions.StateVertex destination) {
		GraphPath<Transitions.Vertex, DefaultEdge> ret = DijkstraShortestPath.findPathBetween(routesAsGraph, source, destination);
		return ret != null && !ret.getEdgeList().isEmpty();
	}

	@SuppressWarnings("unchecked")
	private static <D> State<D> stateOfMap(Map<StateID<?>, State<?>> stateMap, StateID<D> destination) {
		return (State<D>) stateMap.get(destination);
	}

	public static class ReachedState<D> implements AutoCloseable {

		private final State<D> state;
		private final List<Collection<NamedTypeAndState<?>>> initializedStates;
		private final Map<StateID<?>, State<?>> stateMap;
		private final TransitionWalker parent;
		private final List<Listener> initListener;

		private ReachedState(TransitionWalker parent, List<Collection<NamedTypeAndState<?>>> initializedStates, Map<StateID<?>, State<?>> stateMap,
			State<D> state, List<Listener> initListener) {
			this.parent = parent;
			this.state = state;
			this.initListener = Preconditions.checkNotNull(initListener, "initListener is null");
			this.stateMap = new LinkedHashMap<>(stateMap);
			this.initializedStates = new ArrayList<>(initializedStates);
		}

		public <T> ReachedState<T> initState(StateID<T> destination) {
			return parent.initState(stateMap, destination, initListener);
		}

		@Override
		public void close() {
			tearDown(initializedStates, initListener);
		}

		public D current() {
			return state.value();
		}

		public State<D> asState() {
			return State.builder(current())
				.onTearDown(current -> close())
				.build();
		}
	}

	private static void tearDown(List<Collection<NamedTypeAndState<?>>> initializedStates, List<Listener> initListener) {
		List<RuntimeException> exceptions = new ArrayList<>();

		ArrayList<Collection<NamedTypeAndState<?>>> copy = new ArrayList<>(initializedStates);
		Collections.reverse(copy);

		copy.forEach(stateSet -> stateSet.forEach(typeAndState -> {
			notifyListener(initListener, typeAndState);
			try {
				tearDown(typeAndState.state());
			}
			catch (RuntimeException rx) {
				exceptions.add(rx);
			}
		}));

		if (!exceptions.isEmpty()) {
			if (exceptions.size() == 1) {
				throw new TearDownException("tearDown errors", exceptions.get(0));
			}
			throw new TearDownException("tearDown errors", exceptions);
		}
	}

	private static Collection<NamedTypeAndState<?>> asNamedTypeAndState(Map<StateID<?>, State<?>> newStatesAsMap) {
		return newStatesAsMap.entrySet().stream()
			.map(TransitionWalker::namedTypeAndStateOf)
			.collect(Collectors.toList());
	}

	private static NamedTypeAndState<?> namedTypeAndStateOf(Map.Entry<StateID<?>, State<?>> e) {
		return NamedTypeAndState.of((StateID) e.getKey(), e.getValue());
	}

	private static <T> void notifyListener(List<Listener> initListener, NamedTypeAndState<T> typeAndState) {
		initListener.forEach(listener -> listener.onStateTearDown(typeAndState.type(), typeAndState.state().value()));
	}

	private static <T> Set<T> filterNotIn(Set<T> existing, Set<T> toFilter) {
		return toFilter.stream()
			.filter(t -> !existing.contains(t))
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private static <D> void tearDown(State<D> state) {
		state.onTearDown().ifPresent(t -> t.onTearDown(state.value()));
	}

	public static TransitionWalker with(List<? extends Transition<?>> src) {
		ArrayList<Transition<?>> routes = new ArrayList<>(src);

		DefaultDirectedGraph<Transitions.Vertex, DefaultEdge> graph = Transitions.asGraph(routes);
		List<? extends Loop<Transitions.Vertex, DefaultEdge>> loops = Graphs.loopsOf(graph);

		Preconditions.checkArgument(loops.isEmpty(), "loops are not supported: %s", Preconditions.lazy(() -> asMessage(loops)));

		return new TransitionWalker(graph);
	}

	private static String asMessage(List<? extends Loop<Transitions.Vertex, DefaultEdge>> loops) {
		return loops.stream().map(TransitionWalker::asMessage).reduce((l, r) -> l + "\n" + r).orElse("");
	}

	private static String asMessage(Loop<Transitions.Vertex, DefaultEdge> loop) {
		return loop.vertexSet().stream()
			.map(TransitionWalker::asMessage)
			.reduce((l, r) -> l + "->" + r)
			.get();
	}

	private static String asMessage(Collection<StateID<?>> types) {
		return types.stream()
			.map(TransitionWalker::asMessage)
			.reduce((l, r) -> l + ", " + r)
			.orElse("");
	}

	private static String asMessage(Transitions.Vertex type) {
		return Transitions.asEither(type)
			.mapLeft(Transitions.StateVertex::stateId)
			.mapLeft(TransitionWalker::asMessage)
			.mapRight(Transitions.TransitionVertex::transition)
			.mapRight(TransitionWalker::asMessage)
			.map(Function.identity(), Function.identity());
	}

	private static String asMessage(StateID<?> type) {
		return "State(" + (type.name().isEmpty() ? typeAsMessage(type.type()) : type.name() + ":" + typeAsMessage(type.type())) + ")";
	}

	private static String asMessage(Transition<?> transition) {
		return transition.toString();
	}

	private static String typeAsMessage(Type type) {
		return type.getTypeName().startsWith(JAVA_LANG_PACKAGE)
			? type.getTypeName().substring(JAVA_LANG_PACKAGE.length())
			: type.getTypeName();
	}
}
