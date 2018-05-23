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
package de.flapdoodle.transition.initlike;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.UnmodifiableDirectedGraph;

import de.flapdoodle.checks.Preconditions;
import de.flapdoodle.graph.Graphs;
import de.flapdoodle.graph.Loop;
import de.flapdoodle.graph.VerticesAndEdges;
import de.flapdoodle.transition.StateID;
import de.flapdoodle.transition.initlike.resolver.StateOfNamedType;
import de.flapdoodle.transition.initlike.resolver.TransitionResolver;
import de.flapdoodle.transition.routes.Route.Transition;
import de.flapdoodle.transition.routes.RoutesAsGraph;
import de.flapdoodle.transition.routes.RoutesAsGraph.RouteAndVertex;
import de.flapdoodle.transition.routes.SingleDestination;

public class InitLike {

	private static final String JAVA_LANG_PACKAGE = "java.lang.";

	private final Context context;

	private InitLike(InitRoutes<SingleDestination<?>> routes, UnmodifiableDirectedGraph<StateID<?>, RoutesAsGraph.RouteAndVertex> routesAsGraph,
			Map<StateID<?>, List<SingleDestination<?>>> routeByDestination) {
		this.context = new Context(routes, routesAsGraph, routeByDestination);
	}

	public <D> Init<D> init(StateID<D> destination, InitListener...listener) {
		return context.init(new LinkedHashMap<>(), destination, Collections.unmodifiableList(Arrays.asList(listener)));
	}

	private static Map<StateID<?>, State<?>> resolve(DirectedGraph<StateID<?>, RoutesAsGraph.RouteAndVertex> routesAsGraph,
			InitRoutes<SingleDestination<?>> routes, Map<StateID<?>, List<SingleDestination<?>>> routeByDestination, Set<StateID<?>> destinations,
			StateOfNamedType stateOfType, List<InitListener> initListener) {
		Map<StateID<?>, State<?>> ret = new LinkedHashMap<>();
		for (StateID<?> destination : destinations) {
			ret.put(destination, resolve(routesAsGraph, routes, routeByDestination, destination, stateOfType, initListener));
		}
		return ret;
	}

	private static <D> State<D> resolve(DirectedGraph<StateID<?>, RoutesAsGraph.RouteAndVertex> routesAsGraph, InitRoutes<SingleDestination<?>> routes,
			Map<StateID<?>, List<SingleDestination<?>>> routeByDestination, StateID<D> destination, StateOfNamedType stateOfType, List<InitListener> initListener) {
		Function<StateOfNamedType, State<D>> resolver = resolverOf(routesAsGraph, routes, routeByDestination, destination);
		State<D> state = resolver.apply(stateOfType);
		NamedTypeAndState<D> typeAndState = NamedTypeAndState.of(destination, state);
		initListener.forEach(listener -> {
			listener.onStateReached(typeAndState.asTypeAndValue());
		});
		return state;
	}

	private static <D> Function<StateOfNamedType, State<D>> resolverOf(DirectedGraph<StateID<?>, RoutesAsGraph.RouteAndVertex> routesAsGraph,
			InitRoutes<SingleDestination<?>> routes, Map<StateID<?>, List<SingleDestination<?>>> routeByDestination, StateID<D> destination) {
		Preconditions.checkArgument(routesAsGraph.containsVertex(destination), "routes does not contain %s", asMessage(destination));
		SingleDestination<D> route = routeOf(routeByDestination, destination);
		Transition<D> transition = routes.transitionOf(route);
		return resolverOf(route, transition);
	}

	private static Collection<VerticesAndEdges<StateID<?>, RouteAndVertex>> dependenciesOf(
			DirectedGraph<StateID<?>, RoutesAsGraph.RouteAndVertex> routesAsGraph, StateID<?> destination) {
		DirectedGraph<StateID<?>, RouteAndVertex> filtered = Graphs.filter(routesAsGraph,
				v -> v.equals(destination) || isDependencyOf(routesAsGraph, v, destination));
		Collection<VerticesAndEdges<StateID<?>, RouteAndVertex>> roots = Graphs.rootsOf(filtered);
		// System.out.println("dependencies -> ");
		// roots.forEach(System.out::println);
		return roots;
	}

	private static boolean isDependencyOf(DirectedGraph<StateID<?>, RoutesAsGraph.RouteAndVertex> routesAsGraph, StateID<?> source,
			StateID<?> destination) {
		List<RouteAndVertex> ret = DijkstraShortestPath.findPathBetween(routesAsGraph, source, destination);
		return ret != null && !ret.isEmpty();
	}

	private static void printGraphAsDot(DirectedGraph<StateID<?>, RoutesAsGraph.RouteAndVertex> routesAsGraph) {
		String dot = RoutesAsGraph.routeGraphAsDot("init", routesAsGraph);
		System.out.println("---------------------");
		System.out.println(dot);
		System.out.println("---------------------");
	}

	private static <D> Function<StateOfNamedType, State<D>> resolverOf(SingleDestination<D> route, Transition<D> transition) {
		Optional<Function<StateOfNamedType, State<D>>> optResolver = TransitionResolver.resolverOf(TransitionResolver.defaultResolvers(), route, transition);
		Preconditions.checkArgument(optResolver.isPresent(), "could not find resolver for %s(%s)", route, transition);
		Function<StateOfNamedType, State<D>> resolver = optResolver.get();
		return resolver;
	}

	@SuppressWarnings("unchecked")
	private static <D> SingleDestination<D> routeOf(Map<StateID<?>, List<SingleDestination<?>>> routeByDestination, StateID<D> destination) {
		List<SingleDestination<?>> routeForThisDestination = routeByDestination.get(destination);
		Preconditions.checkArgument(routeForThisDestination != null, "found no route to %s", destination);
		Preconditions.checkArgument(!routeForThisDestination.isEmpty(), "found no route to %s", destination);
		Preconditions.checkArgument(routeForThisDestination.size() == 1, "found more than one route to %s: %s", destination, routeForThisDestination);
		return (SingleDestination<D>) routeForThisDestination.get(0);
	}

	private static class Context {

		private final InitRoutes<SingleDestination<?>> routes;
		private final UnmodifiableDirectedGraph<StateID<?>, RoutesAsGraph.RouteAndVertex> routesAsGraph;
		private final Map<StateID<?>, List<SingleDestination<?>>> routeByDestination;

		private Context(InitRoutes<SingleDestination<?>> routes, UnmodifiableDirectedGraph<StateID<?>, RoutesAsGraph.RouteAndVertex> routesAsGraph,
				Map<StateID<?>, List<SingleDestination<?>>> routeByDestination) {
			this.routes = routes;
			this.routesAsGraph = routesAsGraph;
			this.routeByDestination = routeByDestination;
		}

		private <D> Init<D> init(Map<StateID<?>, State<?>> currentStateMap, StateID<D> destination, List<InitListener> initListener) {
			Preconditions.checkArgument(!currentStateMap.containsKey(destination), "state %s already initialized", asMessage(destination));
			Preconditions.checkArgument(routesAsGraph.containsVertex(destination), "state %s is not part of this init process", asMessage(destination));
			// printGraphAsDot(routesAsGraph);

			Map<StateID<?>, State<?>> stateMap = new LinkedHashMap<>(currentStateMap);
			List<Collection<NamedTypeAndState<?>>> initializedStates = new ArrayList<>();

			Collection<VerticesAndEdges<StateID<?>, RouteAndVertex>> dependencies = dependenciesOf(routesAsGraph, destination);
			for (VerticesAndEdges<StateID<?>, RouteAndVertex> set : dependencies) {
				Set<StateID<?>> needInitialization = filterNotIn(stateMap.keySet(), set.vertices());
				try {
					Map<StateID<?>, State<?>> newStatesAsMap = resolve(routesAsGraph, routes, routeByDestination, needInitialization,
							new MapBasedStateOfNamedType(stateMap), initListener);
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

			Collections.reverse(initializedStates);

			return new Init<D>(this, initializedStates, stateMap, destination, stateOfMap(stateMap, destination), initListener);
		}

		@SuppressWarnings("unchecked")
		private static <D> State<D> stateOfMap(Map<StateID<?>, State<?>> stateMap, StateID<D> destination) {
			return (State<D>) stateMap.get(destination);
		}
	}

	public static class Init<D> implements AutoCloseable {

		private final StateID<D> destination;
		private final State<D> state;
		private final List<Collection<NamedTypeAndState<?>>> initializedStates;
		private final Map<StateID<?>, State<?>> stateMap;
		private final Context context;
		private final List<InitListener> initListener;

		private Init(Context context, List<Collection<NamedTypeAndState<?>>> initializedStates, Map<StateID<?>, State<?>> stateMap, StateID<D> destination,
				State<D> state, List<InitListener> initListener) {
			this.context = context;
			this.destination = destination;
			this.state = state;
			this.initListener = Preconditions.checkNotNull(initListener,"initListener is null");
			this.stateMap = new LinkedHashMap<>(stateMap);
			this.initializedStates = new ArrayList<>(initializedStates);
		}

		public <T> Init<T> init(StateID<T> destination) {
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

		initializedStates.forEach(stateSet -> {
			stateSet.forEach(typeAndState -> {
				notifyListener(initListener, typeAndState);
				try {
					tearDown(typeAndState.state());
				}
				catch (RuntimeException rx) {
					exceptions.add(rx);
				}
			});
		});

		if (!exceptions.isEmpty()) {
			if (exceptions.size() == 1) {
				throw new TearDownException("tearDown errors", exceptions.get(0));
			}
			throw new TearDownException("tearDown errors", exceptions);
		}
	}

	private static Collection<NamedTypeAndState<?>> asNamedTypeAndState(Map<StateID<?>, State<?>> newStatesAsMap) {
		return newStatesAsMap.entrySet().stream()
				.map(e -> namedTypeAndStateOf(e))
				.collect(Collectors.toList());
	}

	private static NamedTypeAndState<?> namedTypeAndStateOf(Entry<StateID<?>, State<?>> e) {
		return NamedTypeAndState.of((StateID) e.getKey(), e.getValue());
	}

	private static <T> void notifyListener(List<InitListener> initListener, NamedTypeAndState<T> typeAndState) {
		initListener.forEach(listener -> {
			listener.onStateTearDown(typeAndState.asTypeAndValue());
		});
	}

	private static <T> Set<T> filterNotIn(Set<T> existing, Set<T> toFilter) {
		return new LinkedHashSet<>(toFilter.stream()
				.filter(t -> !existing.contains(t))
				.collect(Collectors.toList()));
	}

	private static <D> void tearDown(State<D> state) {
		state.onTearDown().ifPresent(t -> t.onTearDown(state.value()));
	}

	public static InitLike with(InitRoutes<SingleDestination<?>> routes) {
		UnmodifiableDirectedGraph<StateID<?>, RoutesAsGraph.RouteAndVertex> routesAsGraph = RoutesAsGraph.asGraph(routes.all());
		List<? extends Loop<StateID<?>, RoutesAsGraph.RouteAndVertex>> loops = Graphs.loopsOf(routesAsGraph);

		Preconditions.checkArgument(loops.isEmpty(), "loops are not supported: %s", Preconditions.lazy(() -> asMessage(loops)));

		Map<StateID<?>, List<SingleDestination<?>>> routeByDestination = routes.all().stream()
				.collect(Collectors.groupingBy(r -> r.destination()));

		return new InitLike(routes, routesAsGraph, routeByDestination);
	}

	private static String asMessage(List<? extends Loop<StateID<?>, ?>> loops) {
		return loops.stream().map(l -> asMessage(l)).reduce((l, r) -> l + "\n" + r).orElse("");
	}

	private static String asMessage(Loop<StateID<?>, ?> loop) {
		return loop.vertexSet().stream().map(v -> asMessage(v)).reduce((l, r) -> l + "->" + r).get();
	}

	private static String asMessage(Collection<StateID<?>> types) {
		return types.stream().map(v -> asMessage(v)).reduce((l, r) -> l + ", " + r).get();
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
