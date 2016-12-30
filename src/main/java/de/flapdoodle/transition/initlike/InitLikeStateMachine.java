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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.UnmodifiableDirectedGraph;

import de.flapdoodle.graph.Graphs;
import de.flapdoodle.graph.Loop;
import de.flapdoodle.transition.NamedType;
import de.flapdoodle.transition.State;
import de.flapdoodle.transition.TearDown;
import de.flapdoodle.transition.routes.Routes;
import de.flapdoodle.transition.routes.RoutesAsGraph;
import de.flapdoodle.transition.routes.SingleDestination;

public class InitLikeStateMachine {

	private final Routes<SingleDestination<?>> routes;
	private final Map<NamedType<?>, List<SingleDestination<?>>> availableDestinations;
	private static final List<TransitionResolver> transitionResolvers = Arrays.asList(
			new StartResolver(),
			new BridgeResolver(),
			new MergingJunctionResolver());

	public InitLikeStateMachine(Routes<SingleDestination<?>> routes, Map<NamedType<?>, List<SingleDestination<?>>> availableDestinations) {
		this.routes = routes;
		this.availableDestinations = availableDestinations;
	}
	
	public <T> AutocloseableState<T> init(NamedType<T> type) {
		List<SingleDestination<?>> possibleRoutes = Objects.requireNonNull(availableDestinations.get(type),() -> "no route to "+type+" found");
		if (possibleRoutes.size()>1) {
			throw new IllegalArgumentException("there are more than one way to get here: "+type);
		}
		SingleDestination<T> route = (SingleDestination<T>) possibleRoutes.get(0);
		
		for (TransitionResolver resolver : transitionResolvers) {
			Optional<Function<StateResolver, State<T>>> resolvedTransition = resolver.resolve(route, routes.transitionOf(route));
			if (resolvedTransition.isPresent()) {
				CollectingStatesStateResolver stateResolver = new CollectingStatesStateResolver();
				State<T> state = resolvedTransition.get().apply(stateResolver);
				return wrap(state,stateResolver.collectedStates());
			}
		}
		
		throw new IllegalArgumentException("could not resolve: "+type);
	}
	
	private <T> AutocloseableState<T> wrap(State<T> src, List<AutocloseableState<?>> dependingStates) {
		return new AutocloseableWrapper<T>(src, dependingStates);
	}

	public static InitLikeStateMachine with(Routes<SingleDestination<?>> routes) {
		UnmodifiableDirectedGraph<NamedType<?>, DefaultEdge> routesAsGraph=RoutesAsGraph.asGraph(routes.all());
		List<? extends Loop<NamedType<?>, DefaultEdge>> loops = Graphs.loopsOf(routesAsGraph);
		
		if (!loops.isEmpty()) {
			throw new IllegalArgumentException("loops are not supported: "+asMessage(loops));
		}
		
		Map<NamedType<?>, List<SingleDestination<?>>> availableDestinations = routes.all().stream()
			.collect(Collectors.groupingBy(r -> r.destination()));
		
		return new InitLikeStateMachine(routes, availableDestinations);
	}

	private static String asMessage(List<? extends Loop<NamedType<?>, DefaultEdge>> loops) {
		return loops.stream().map(l -> asMessage(l)).reduce((l,r) -> l+"\n"+r).orElse("");
	}

	private static String asMessage(Loop<NamedType<?>, DefaultEdge> loop) {
		return loop.vertexSet().stream().map(v -> v.toString()).reduce((l,r) -> l+"->"+r).get();
	}

	private static class AutocloseableWrapper<T> implements AutocloseableState<T> {

		private final State<T> wrapping;
		private final List<AutocloseableState<?>> dependingStates;

		public AutocloseableWrapper(State<T> wrapping, List<AutocloseableState<?>> dependingStates) {
			this.wrapping = wrapping;
			this.dependingStates = dependingStates;
		}
		
		@Override
		public T current() {
			return wrapping.current();
		}

		@Override
		public Optional<TearDown<T>> onTearDown() {
			return wrapping.onTearDown().map(t -> t.andThen(x -> tearDownDependendStates()));
		}
		
		@Override
		public void close() throws RuntimeException {
			tearDown(this);
		}
		
		private void tearDownDependendStates() {
			dependingStates.forEach(d -> tearDown(d));
		}

		private static <T> void tearDown(AutocloseableState<T> state) {
			state.onTearDown().ifPresent(t -> t.onTearDown(state.current()));
		}
	}
	
	private class CollectingStatesStateResolver implements StateResolver {

		List<AutocloseableState<?>> collectedStates=new ArrayList<>();
		
		@Override
		public <D> State<D> resolve(NamedType<D> type) {
			AutocloseableState<D> state = init(type);
			collectedStates.add(state);
			return state;
		}
		
		public List<AutocloseableState<?>> collectedStates() {
			return collectedStates;
		}
		
	}
}
