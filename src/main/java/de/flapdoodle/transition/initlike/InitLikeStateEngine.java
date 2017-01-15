package de.flapdoodle.transition.initlike;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jgrapht.graph.UnmodifiableDirectedGraph;

import de.flapdoodle.graph.Graphs;
import de.flapdoodle.graph.Loop;
import de.flapdoodle.transition.NamedType;
import de.flapdoodle.transition.State;
import de.flapdoodle.transition.routes.Route.Transition;
import de.flapdoodle.transition.routes.Routes;
import de.flapdoodle.transition.routes.RoutesAsGraph;
import de.flapdoodle.transition.routes.SingleDestination;

public class InitLikeStateEngine {

	private final Routes<SingleDestination<?>> routes;
	private final Map<NamedType<?>, List<SingleDestination<?>>> availableDestinations;
	private final InitState<Void> baseState;
	private static final Collection<TransitionResolver> transitionResolvers = TransitionResolver.defaultResolvers();
	private final UnmodifiableDirectedGraph<NamedType<?>, RoutesAsGraph.RouteAndVertex> routesAsGraph;

	private InitLikeStateEngine(Routes<SingleDestination<?>> routes, UnmodifiableDirectedGraph<NamedType<?>,RoutesAsGraph.RouteAndVertex> routesAsGraph, Map<NamedType<?>, List<SingleDestination<?>>> availableDestinations) {
		this.routes = routes;
		this.routesAsGraph = routesAsGraph;
		this.availableDestinations = availableDestinations;
		this.baseState=new InitState<Void>(routes, routesAsGraph, availableDestinations);
	}
	
	public <T> InitState<T> init(NamedType<T> type) {
		return baseState.init(type);
	}
	
	public static InitLikeStateEngine with(Routes<SingleDestination<?>> routes) {
		UnmodifiableDirectedGraph<NamedType<?>, RoutesAsGraph.RouteAndVertex> routesAsGraph = RoutesAsGraph.asGraph(routes.all());
		List<? extends Loop<NamedType<?>, RoutesAsGraph.RouteAndVertex>> loops = Graphs.loopsOf(routesAsGraph);
		
		if (!loops.isEmpty()) {
			throw new IllegalArgumentException("loops are not supported: "+asMessage(loops));
		}
		
		Map<NamedType<?>, List<SingleDestination<?>>> availableDestinations = routes.all().stream()
				.collect(Collectors.groupingBy(r -> r.destination()));

		return new InitLikeStateEngine(routes, routesAsGraph, availableDestinations);
	}
	
	public static class InitState<T> implements AutoCloseable {

		private final Routes<SingleDestination<?>> routes;
		private final Map<NamedType<?>, List<SingleDestination<?>>> availableDestinations;
		private final UnmodifiableDirectedGraph<NamedType<?>, RoutesAsGraph.RouteAndVertex> routesAsGraph;

		public InitState(Routes<SingleDestination<?>> routes, UnmodifiableDirectedGraph<NamedType<?>,RoutesAsGraph.RouteAndVertex> routesAsGraph, Map<NamedType<?>, List<SingleDestination<?>>> availableDestinations) {
			this.routes = routes;
			this.routesAsGraph = routesAsGraph;
			this.availableDestinations = availableDestinations;
		}

		public <T> InitState<T> init(NamedType<T> type) {
			List<Set<NamedType<?>>> dependencies = dependencySetsOf(routesAsGraph, type);
			System.out.println("Dep -> "+dependencies);
			
			if (dependencies.isEmpty()) {
				SingleDestination<T> route = routeTo(availableDestinations, type);
				Transition<T> transition = routes.transitionOf(route);
				
			}
//			
//			SingleDestination<T> route = routeTo(availableDestinations, type);
//			Transition<T> transition = routes.transitionOf(route);
//			
//			
//			Optional<Function<StateResolver, State<T>>> resolver = resolverOf(transitionResolvers, route, transition);
//			if (resolver.isPresent()) {
////				CollectingStatesStateResolver stateResolver = new CollectingStatesStateResolver();
////				State<T> state = resolver.get().apply(stateResolver);
////				return wrap(state,stateResolver.collectedStates());
//			}
			
			throw new IllegalArgumentException("could not resolve: "+type);
		}

		private static <T> List<Set<NamedType<?>>> dependencySetsOf(UnmodifiableDirectedGraph<NamedType<?>, RoutesAsGraph.RouteAndVertex> routesAsGraph, NamedType<T> type) {
			ArrayList<Set<NamedType<?>>> ret = new ArrayList<>();
			Set<NamedType<?>> dep = routesAsGraph.outgoingEdgesOf(type).stream()
						.map(e -> routesAsGraph.getEdgeTarget(e))
						.collect(Collectors.toSet());
			if (!dep.isEmpty()) {
				ret.add(dep);
			}
			dep.forEach(d -> {
				ret.addAll(dependencySetsOf(routesAsGraph, d));
			});
			return ret;
		}
		
		@Override
		public void close() throws RuntimeException {
			
		}
		
	}
	
	private static class StateFactory {
		
	}
	
	private static class CollectingStatesStateResolver implements StateOfNamedType {

		private final List<State<?>> collectedStates=new ArrayList<>();
		private final Function<NamedType<?>, State<?>> init;
		
		public CollectingStatesStateResolver(Function<NamedType<?>, State<?>> init) {
			this.init = init;
			// TODO Auto-generated constructor stub
		}
		
		@Override
		public <D> State<D> of(NamedType<D> type) {
			State<D> state = (State<D>) init.apply(type);
			collectedStates.add(state);
			return state;
		}
		
		public List<State<?>> collectedStates() {
			return collectedStates;
		}
		
	}

	
	private static String asMessage(List<? extends Loop<NamedType<?>, ?>> loops) {
		return loops.stream().map(l -> asMessage(l)).reduce((l,r) -> l+"\n"+r).orElse("");
	}

	private static String asMessage(Loop<NamedType<?>, ?> loop) {
		return loop.vertexSet().stream().map(v -> v.toString()).reduce((l,r) -> l+"->"+r).get();
	}

	private static <T> SingleDestination<T> routeTo(Map<NamedType<?>, List<SingleDestination<?>>> availableDestinations, NamedType<T> type) {
		List<SingleDestination<?>> possibleRoutes = Objects.requireNonNull(availableDestinations.get(type),() -> "no route to "+type+" found");
		if (possibleRoutes.size()>1) {
			throw new IllegalArgumentException("there are more than one way to get here: "+type);
		}
		return (SingleDestination<T>) possibleRoutes.get(0);
	}

}
