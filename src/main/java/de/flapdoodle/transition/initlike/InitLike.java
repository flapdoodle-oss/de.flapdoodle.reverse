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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.UnmodifiableDirectedGraph;

import de.flapdoodle.graph.Graphs;
import de.flapdoodle.graph.Loop;
import de.flapdoodle.transition.NamedType;
import de.flapdoodle.transition.Preconditions;
import de.flapdoodle.transition.State;
import de.flapdoodle.transition.routes.Route.Transition;
import de.flapdoodle.transition.routes.Routes;
import de.flapdoodle.transition.routes.RoutesAsGraph;
import de.flapdoodle.transition.routes.SingleDestination;

public class InitLike {

	private final Routes<SingleDestination<?>> routes;
	private final UnmodifiableDirectedGraph<NamedType<?>, RoutesAsGraph.RouteAndVertex> routesAsGraph;
	private final Map<NamedType<?>, List<SingleDestination<?>>> routeByDestination;

	private InitLike(Routes<SingleDestination<?>> routes, UnmodifiableDirectedGraph<NamedType<?>, RoutesAsGraph.RouteAndVertex> routesAsGraph, Map<NamedType<?>, List<SingleDestination<?>>> routeByDestination) {
		this.routes = routes;
		this.routesAsGraph = routesAsGraph;
		this.routeByDestination = routeByDestination;
	}
	
	public <D> Init<D> init(NamedType<D> destination) {
		printGraphAsDot(routesAsGraph);
		
		Preconditions.checkArgument(routesAsGraph.containsVertex(destination), "routes does not contain %s", destination);
		SingleDestination<D> route = routeOf(routeByDestination, destination);
		Transition<D> transition = routes.transitionOf(route);
		Set<RoutesAsGraph.RouteAndVertex> dependsOn = routesAsGraph.incomingEdgesOf(destination);
		
		if (dependsOn.isEmpty()) {
			Function<StateOfNamedType, State<D>> resolver = resolverOf(route, transition);
			State<D> ret = resolver.apply(new MapBasedStateOfNamedType(new LinkedHashMap<>()));
			
			System.out.println("->"+ret);
			
			return new Init(destination, ret);
		} else {
			
		}
		
		throw new IllegalArgumentException("not implemented yet: "+destination+" dep: "+dependsOn);
	}
	
	private static void printGraphAsDot(DirectedGraph<NamedType<?>, RoutesAsGraph.RouteAndVertex> routesAsGraph) {
		String dot=RoutesAsGraph.routeGraphAsDot("init", routesAsGraph);
		System.out.println("---------------------");
		System.out.println(dot);
		System.out.println("---------------------");
	}

	private static <D> Function<StateOfNamedType, State<D>> resolverOf(SingleDestination<D> route, Transition<D> transition) {
		Optional<Function<StateOfNamedType, State<D>>> optResolver = TransitionResolver.resolverOf(TransitionResolver.defaultResolvers(),route,transition);
		Preconditions.checkArgument(optResolver.isPresent(), "could not find resolver for %s(%s)",route,transition);
		Function<StateOfNamedType, State<D>> resolver = optResolver.get();
		return resolver;
	}

	private static <D> SingleDestination<D> routeOf(Map<NamedType<?>, List<SingleDestination<?>>> routeByDestination, NamedType<D> destination) {
		List<SingleDestination<?>> routeForThisDestination = routeByDestination.get(destination);
		Preconditions.checkArgument((routeForThisDestination!=null) && (routeForThisDestination.size()==1), "more ore less than one route to %s -> %s",destination,routeForThisDestination);
		return (SingleDestination<D>) routeForThisDestination.get(0);
	}
	
	private static class MapBasedStateOfNamedType implements StateOfNamedType {

		private final Map<NamedType<?>, State<?>> stateMap;

		public MapBasedStateOfNamedType(Map<NamedType<?>, State<?>> stateMap) {
			this.stateMap = new LinkedHashMap<>(stateMap);
		}
		
		@Override
		public <D> State<D> of(NamedType<D> type) {
			return (State<D>) Preconditions.checkNotNull(stateMap.get(type),"could find state for %s", type);
		}
		
	}
	
	
	
	public class Init<D> implements AutoCloseable {

		private final NamedType<D> destination;
		private final State<D> state;

		private Init(NamedType<D> destination, State<D> state) {
			this.destination = destination;
			this.state = state;
		}

		@Override
		public void close() {
			state.onTearDown().ifPresent(t -> t.onTearDown(state.current()));
		}

		public D current() {
			return state.current();
		}
	}

	public static InitLike with(Routes<SingleDestination<?>> routes) {
		UnmodifiableDirectedGraph<NamedType<?>, RoutesAsGraph.RouteAndVertex> routesAsGraph = RoutesAsGraph.asGraph(routes.all());
		List<? extends Loop<NamedType<?>, RoutesAsGraph.RouteAndVertex>> loops = Graphs.loopsOf(routesAsGraph);
		
		Preconditions.checkArgument(loops.isEmpty(), "loops are not supported: %s",Preconditions.lazy(() -> asMessage(loops)));
		
		Map<NamedType<?>, List<SingleDestination<?>>> routeByDestination = routes.all().stream()
				.collect(Collectors.groupingBy(r -> r.destination()));
		
		return new InitLike(routes, routesAsGraph, routeByDestination);
	}
	
	private static String asMessage(List<? extends Loop<NamedType<?>, ?>> loops) {
		return loops.stream().map(l -> asMessage(l)).reduce((l,r) -> l+"\n"+r).orElse("");
	}

	private static String asMessage(Loop<NamedType<?>, ?> loop) {
		return loop.vertexSet().stream().map(v -> v.toString()).reduce((l,r) -> l+"->"+r).get();
	}
}
