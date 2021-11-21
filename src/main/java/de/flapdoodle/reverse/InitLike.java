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
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultDirectedGraph;

import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class InitLike {
	private static final String JAVA_LANG_PACKAGE = "java.lang.";

	private final Context context;

	private InitLike(
		DefaultDirectedGraph<StateID<?>, TransitionsAsGraph.EdgeAndVertex> edgesAsGraph,
		Map<StateID<?>, List<Transition<?>>> routeByDestination) {

		this.context = new Context(edgesAsGraph, routeByDestination);
	}

	public <D> InitLike.ReachedState<D> init(StateID<D> destination, InitListener... listener) {
		return context.init(new LinkedHashMap<>(), destination, Collections.unmodifiableList(Arrays.asList(listener)));
	}

	private static Map<StateID<?>, State<?>> resolve(DefaultDirectedGraph<StateID<?>, TransitionsAsGraph.EdgeAndVertex> routesAsGraph,
		Map<StateID<?>, List<Transition<?>>> routeByDestination, Set<StateID<?>> destinations,
		StateLookup stateOfType, List<InitListener> initListener) {
		Map<StateID<?>, State<?>> ret = new LinkedHashMap<>();
		for (StateID<?> destination : destinations) {
			ret.put(destination, resolve(routesAsGraph, routeByDestination, destination, stateOfType, initListener));
		}
		return ret;
	}

	private static <D> State<D> resolve(DefaultDirectedGraph<StateID<?>, TransitionsAsGraph.EdgeAndVertex> routesAsGraph,
		Map<StateID<?>, List<Transition<?>>> routeByDestination, StateID<D> destination, StateLookup stateOfType, List<InitListener> initListener) {
		Function<StateLookup, State<D>> resolver = resolverOf(routesAsGraph, routeByDestination, destination);
		State<D> state = resolver.apply(stateOfType);
		initListener.forEach(listener -> listener.onStateReached(destination, state.value()));
		return state;
	}

	private static <D> Function<StateLookup, State<D>> resolverOf(DefaultDirectedGraph<StateID<?>, TransitionsAsGraph.EdgeAndVertex> routesAsGraph,
		Map<StateID<?>, List<Transition<?>>> routeByDestination, StateID<D> destination) {
		Preconditions.checkArgument(routesAsGraph.containsVertex(destination), "routes does not contain %s", asMessage(destination));
		Transition<D> route = routeOf(routeByDestination, destination);
		return route::result;
	}

	private static Collection<VerticesAndEdges<StateID<?>, TransitionsAsGraph.EdgeAndVertex>> dependenciesOf(
		DefaultDirectedGraph<StateID<?>, TransitionsAsGraph.EdgeAndVertex> routesAsGraph, StateID<?> destination) {
		DefaultDirectedGraph<StateID<?>, TransitionsAsGraph.EdgeAndVertex> filtered = Graphs.filter(routesAsGraph,
			v -> v.equals(destination) || isDependencyOf(routesAsGraph, v, destination));
		Collection<VerticesAndEdges<StateID<?>, TransitionsAsGraph.EdgeAndVertex>> roots = Graphs.rootsOf(filtered);
		// System.out.println("dependencies -> ");
		// roots.forEach(System.out::println);
		return roots;
	}

	private static boolean isDependencyOf(DefaultDirectedGraph<StateID<?>, TransitionsAsGraph.EdgeAndVertex> routesAsGraph, StateID<?> source,
		StateID<?> destination) {
		GraphPath<StateID<?>, TransitionsAsGraph.EdgeAndVertex> ret = DijkstraShortestPath.findPathBetween(routesAsGraph, source, destination);
		return ret != null && !ret.getEdgeList().isEmpty();
	}

	private static void printGraphAsDot(DefaultDirectedGraph<StateID<?>, TransitionsAsGraph.EdgeAndVertex> routesAsGraph) {
		String dot = TransitionsAsGraph.edgeGraphAsDot("init", routesAsGraph);
		System.out.println("---------------------");
		System.out.println(dot);
		System.out.println("---------------------");
	}

	@SuppressWarnings("unchecked")
	private static <D> Transition<D> routeOf(Map<StateID<?>, List<Transition<?>>> routeByDestination, StateID<D> destination) {
		List<Transition<?>> routeForThisDestination = routeByDestination.get(destination);
		Preconditions.checkArgument(routeForThisDestination != null, "found no route to %s", destination);
		Preconditions.checkArgument(!routeForThisDestination.isEmpty(), "found no route to %s", destination);
		Preconditions.checkArgument(routeForThisDestination.size() == 1, "found more than one route to %s: %s", destination, routeForThisDestination);
		return (Transition<D>) routeForThisDestination.get(0);
	}

	private static class Context {

		private final DefaultDirectedGraph<StateID<?>, TransitionsAsGraph.EdgeAndVertex> edgesAsGraph;
		private final Map<StateID<?>, List<Transition<?>>> routeByDestination;

		private Context(
			DefaultDirectedGraph<StateID<?>, TransitionsAsGraph.EdgeAndVertex> edgesAsGraph,
			Map<StateID<?>, List<Transition<?>>> routeByDestination
		) {
			this.edgesAsGraph = edgesAsGraph;
			this.routeByDestination = routeByDestination;
		}

		private <D> ReachedState<D> init(Map<StateID<?>, State<?>> currentStateMap, StateID<D> destination, List<InitListener> initListener) {
			Preconditions.checkArgument(!currentStateMap.containsKey(destination), "state %s already initialized", asMessage(destination));
			Preconditions.checkArgument(edgesAsGraph.containsVertex(destination), "state %s is not part of this init process", asMessage(destination));
			// printGraphAsDot(routesAsGraph);

			Map<StateID<?>, State<?>> stateMap = new LinkedHashMap<>(currentStateMap);
			List<Collection<NamedTypeAndState<?>>> initializedStates = new ArrayList<>();

			Collection<VerticesAndEdges<StateID<?>, TransitionsAsGraph.EdgeAndVertex>> dependencies = dependenciesOf(edgesAsGraph, destination);
			for (VerticesAndEdges<StateID<?>, TransitionsAsGraph.EdgeAndVertex> set : dependencies) {
				Set<StateID<?>> needInitialization = filterNotIn(stateMap.keySet(), set.vertices());
				try {
					Map<StateID<?>, State<?>> newStatesAsMap = resolve(edgesAsGraph, routeByDestination, needInitialization,
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

//						Collections.reverse(initializedStates);

			return new ReachedState<>(this, initializedStates, stateMap, stateOfMap(stateMap, destination), initListener);
		}

		@SuppressWarnings("unchecked")
		private static <D> State<D> stateOfMap(Map<StateID<?>, State<?>> stateMap, StateID<D> destination) {
			return (State<D>) stateMap.get(destination);
		}
	}

	public static class ReachedState<D> implements AutoCloseable {

		private final State<D> state;
		private final List<Collection<NamedTypeAndState<?>>> initializedStates;
		private final Map<StateID<?>, State<?>> stateMap;
		private final Context context;
		private final List<InitListener> initListener;

		private ReachedState(Context context, List<Collection<NamedTypeAndState<?>>> initializedStates, Map<StateID<?>, State<?>> stateMap,
			State<D> state, List<InitListener> initListener) {
			this.context = context;
			this.state = state;
			this.initListener = Preconditions.checkNotNull(initListener, "initListener is null");
			this.stateMap = new LinkedHashMap<>(stateMap);
			this.initializedStates = new ArrayList<>(initializedStates);
		}

		public <T> InitLike.ReachedState<T> init(StateID<T> destination) {
			return context.init(stateMap, destination, initListener);
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

	private static void tearDown(List<Collection<NamedTypeAndState<?>>> initializedStates, List<InitListener> initListener) {
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
			.map(InitLike::namedTypeAndStateOf)
			.collect(Collectors.toList());
	}

	private static NamedTypeAndState<?> namedTypeAndStateOf(Map.Entry<StateID<?>, State<?>> e) {
		return NamedTypeAndState.of((StateID) e.getKey(), e.getValue());
	}

	private static <T> void notifyListener(List<InitListener> initListener, NamedTypeAndState<T> typeAndState) {
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

	public static InitLike with(List<? extends Transition<?>> src) {
		ArrayList<Transition<?>> routes = new ArrayList<>(src);

		DefaultDirectedGraph<StateID<?>, TransitionsAsGraph.EdgeAndVertex> edgesAsGraph = TransitionsAsGraph.asGraph(routes);
		List<? extends Loop<StateID<?>, TransitionsAsGraph.EdgeAndVertex>> loops = Graphs.loopsOf(edgesAsGraph);

		Preconditions.checkArgument(loops.isEmpty(), "loops are not supported: %s", Preconditions.lazy(() -> asMessage(loops)));

		Map<StateID<?>, List<Transition<?>>> routeByDestination = routes.stream()
			.collect(Collectors.groupingBy(Transition::destination));

		return new InitLike(edgesAsGraph, routeByDestination);
	}

	private static String asMessage(List<? extends Loop<StateID<?>, ?>> loops) {
		return loops.stream().map(InitLike::asMessage).reduce((l, r) -> l + "\n" + r).orElse("");
	}

	private static String asMessage(Loop<StateID<?>, ?> loop) {
		return loop.vertexSet().stream()
			.map(InitLike::asMessage)
			.reduce((l, r) -> l + "->" + r)
			.get();
	}

	private static String asMessage(Collection<StateID<?>> types) {
		return types.stream()
			.map(InitLike::asMessage)
			.reduce((l, r) -> l + ", " + r)
			.get();
	}

	private static String asMessage(StateID<?> type) {
		return "NamedType(" + (type.name().isEmpty() ? typeAsMessage(type.type()) : type.name() + ":" + typeAsMessage(type.type())) + ")";
	}

	private static String typeAsMessage(Type type) {
		return type.getTypeName().startsWith(JAVA_LANG_PACKAGE)
			? type.getTypeName().substring(JAVA_LANG_PACKAGE.length())
			: type.getTypeName();
	}
}
