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
package de.flapdoodle.transition.init;

import de.flapdoodle.checks.Preconditions;
import de.flapdoodle.graph.Graphs;
import de.flapdoodle.graph.Loop;
import de.flapdoodle.graph.VerticesAndEdges;
import de.flapdoodle.transition.StateID;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultDirectedGraph;

import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Init {
		private static final String JAVA_LANG_PACKAGE = "java.lang.";

		private final Context context;

		private Init(
				ArrayList<Edge<?>> routes,
				DefaultDirectedGraph<StateID<?>, EdgesAsGraph.EdgeAndVertex> edgesAsGraph,
				Map<StateID<?>, List<Edge<?>>> routeByDestination) {

				this.context = new Context(routes, edgesAsGraph, routeByDestination);
		}

		public <D> Init.ReachedState<D> init(StateID<D> destination, InitListener...listener) {
				return context.init(new LinkedHashMap<>(), destination, Collections.unmodifiableList(Arrays.asList(listener)));
		}

		private static Map<StateID<?>, State<?>> resolve(DefaultDirectedGraph<StateID<?>, EdgesAsGraph.EdgeAndVertex> routesAsGraph,
				List<Edge<?>> routes, Map<StateID<?>, List<Edge<?>>> routeByDestination, Set<StateID<?>> destinations,
				StateOfNamedType stateOfType, List<InitListener> initListener) {
				Map<StateID<?>, State<?>> ret = new LinkedHashMap<>();
				for (StateID<?> destination : destinations) {
						ret.put(destination, resolve(routesAsGraph, routes, routeByDestination, destination, stateOfType, initListener));
				}
				return ret;
		}

		private static <D> State<D> resolve(DefaultDirectedGraph<StateID<?>, EdgesAsGraph.EdgeAndVertex> routesAsGraph, List<Edge<?>> routes,
				Map<StateID<?>, List<Edge<?>>> routeByDestination, StateID<D> destination, StateOfNamedType stateOfType, List<InitListener> initListener) {
				Function<StateOfNamedType, State<D>> resolver = resolverOf(routesAsGraph, routes, routeByDestination, destination);
				State<D> state = resolver.apply(stateOfType);
				NamedTypeAndState<D> typeAndState = NamedTypeAndState.of(destination, state);
				initListener.forEach(listener -> {
						listener.onStateReached(typeAndState.asTypeAndValue());
				});
				return state;
		}

		private static <D> Function<StateOfNamedType, State<D>> resolverOf(DefaultDirectedGraph<StateID<?>, EdgesAsGraph.EdgeAndVertex> routesAsGraph,
				List<Edge<?>> routes, Map<StateID<?>, List<Edge<?>>> routeByDestination, StateID<D> destination) {
				Preconditions.checkArgument(routesAsGraph.containsVertex(destination), "routes does not contain %s", asMessage(destination));
				Edge<D> route = routeOf(routeByDestination, destination);
				return route.actionHandler();
		}

		private static Collection<VerticesAndEdges<StateID<?>, EdgesAsGraph.EdgeAndVertex>> dependenciesOf(
				DefaultDirectedGraph<StateID<?>, EdgesAsGraph.EdgeAndVertex> routesAsGraph, StateID<?> destination) {
				DefaultDirectedGraph<StateID<?>, EdgesAsGraph.EdgeAndVertex> filtered = Graphs.filter(routesAsGraph,
						v -> v.equals(destination) || isDependencyOf(routesAsGraph, v, destination));
				Collection<VerticesAndEdges<StateID<?>, EdgesAsGraph.EdgeAndVertex>> roots = Graphs.rootsOf(filtered);
				// System.out.println("dependencies -> ");
				// roots.forEach(System.out::println);
				return roots;
		}

		private static boolean isDependencyOf(DefaultDirectedGraph<StateID<?>, EdgesAsGraph.EdgeAndVertex> routesAsGraph, StateID<?> source,
				StateID<?> destination) {
				GraphPath<StateID<?>, EdgesAsGraph.EdgeAndVertex> ret = DijkstraShortestPath.findPathBetween(routesAsGraph, source, destination);
				return ret != null && !ret.getEdgeList().isEmpty();
		}

		private static void printGraphAsDot(DefaultDirectedGraph<StateID<?>, EdgesAsGraph.EdgeAndVertex> routesAsGraph) {
				String dot = EdgesAsGraph.edgeGraphAsDot("init", routesAsGraph);
				System.out.println("---------------------");
				System.out.println(dot);
				System.out.println("---------------------");
		}

		@SuppressWarnings("unchecked")
		private static <D> Edge<D> routeOf(Map<StateID<?>, List<Edge<?>>> routeByDestination, StateID<D> destination) {
				List<Edge<?>> routeForThisDestination = routeByDestination.get(destination);
				Preconditions.checkArgument(routeForThisDestination != null, "found no route to %s", destination);
				Preconditions.checkArgument(!routeForThisDestination.isEmpty(), "found no route to %s", destination);
				Preconditions.checkArgument(routeForThisDestination.size() == 1, "found more than one route to %s: %s", destination, routeForThisDestination);
				return (Edge<D>) routeForThisDestination.get(0);
		}

		private static class Context {

				private final ArrayList<Edge<?>> routes;
				private final DefaultDirectedGraph<StateID<?>, EdgesAsGraph.EdgeAndVertex> edgesAsGraph;
				private final Map<StateID<?>, List<Edge<?>>> routeByDestination;

				private Context(
						ArrayList<Edge<?>> routes,
						DefaultDirectedGraph<StateID<?>, EdgesAsGraph.EdgeAndVertex> edgesAsGraph,
						Map<StateID<?>, List<Edge<?>>> routeByDestination
				) {
						this.routes = routes;
						this.edgesAsGraph = edgesAsGraph;
						this.routeByDestination = routeByDestination;
				}

				private <D> ReachedState<D> init(Map<StateID<?>, State<?>> currentStateMap, StateID<D> destination, List<InitListener> initListener) {
						Preconditions.checkArgument(!currentStateMap.containsKey(destination), "state %s already initialized", asMessage(destination));
						Preconditions.checkArgument(edgesAsGraph.containsVertex(destination), "state %s is not part of this init process", asMessage(destination));
						// printGraphAsDot(routesAsGraph);

						Map<StateID<?>, State<?>> stateMap = new LinkedHashMap<>(currentStateMap);
						List<Collection<NamedTypeAndState<?>>> initializedStates = new ArrayList<>();

						Collection<VerticesAndEdges<StateID<?>, EdgesAsGraph.EdgeAndVertex>> dependencies = dependenciesOf(edgesAsGraph, destination);
						for (VerticesAndEdges<StateID<?>, EdgesAsGraph.EdgeAndVertex> set : dependencies) {
								Set<StateID<?>> needInitialization = filterNotIn(stateMap.keySet(), set.vertices());
								try {
										Map<StateID<?>, State<?>> newStatesAsMap = resolve(edgesAsGraph, routes, routeByDestination, needInitialization,
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

						return new ReachedState<>(this, initializedStates, stateMap, destination, stateOfMap(stateMap, destination), initListener);
				}

				@SuppressWarnings("unchecked")
				private static <D> State<D> stateOfMap(Map<StateID<?>, State<?>> stateMap, StateID<D> destination) {
						return (State<D>) stateMap.get(destination);
				}
		}

		public static class ReachedState<D> implements AutoCloseable {

				private final StateID<D> destination;
				private final State<D> state;
				private final List<Collection<NamedTypeAndState<?>>> initializedStates;
				private final Map<StateID<?>, State<?>> stateMap;
				private final Context context;
				private final List<InitListener> initListener;

				private ReachedState(Context context, List<Collection<NamedTypeAndState<?>>> initializedStates, Map<StateID<?>, State<?>> stateMap, StateID<D> destination,
						State<D> state, List<InitListener> initListener) {
						this.context = context;
						this.destination = destination;
						this.state = state;
						this.initListener = Preconditions.checkNotNull(initListener,"initListener is null");
						this.stateMap = new LinkedHashMap<>(stateMap);
						this.initializedStates = new ArrayList<>(initializedStates);
				}

				public <T> Init.ReachedState<T> init(StateID<T> destination) {
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

		private static NamedTypeAndState<?> namedTypeAndStateOf(Map.Entry<StateID<?>, State<?>> e) {
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


		public static Init with(List<? extends Edge<?>> src) {
				ArrayList<Edge<?>> routes = new ArrayList<>(src);

				DefaultDirectedGraph<StateID<?>, EdgesAsGraph.EdgeAndVertex> edgesAsGraph = EdgesAsGraph.asGraph(routes);
				List<? extends Loop<StateID<?>, EdgesAsGraph.EdgeAndVertex>> loops = Graphs.loopsOf(edgesAsGraph);

				Preconditions.checkArgument(loops.isEmpty(), "loops are not supported: %s", Preconditions.lazy(() -> asMessage(loops)));

				Map<StateID<?>, List<Edge<?>>> routeByDestination = routes.stream()
						.collect(Collectors.groupingBy(Edge::destination));

				return new Init(routes, edgesAsGraph, routeByDestination);
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
