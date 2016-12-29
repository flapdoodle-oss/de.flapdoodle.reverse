package de.flapdoodle.transition.initlike;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.UnmodifiableDirectedGraph;

import de.flapdoodle.graph.Graphs;
import de.flapdoodle.graph.Loop;
import de.flapdoodle.transition.NamedType;
import de.flapdoodle.transition.State;
import de.flapdoodle.transition.routes.Routes;
import de.flapdoodle.transition.routes.SingleDestination;

public class InitLikeStateMachine {

	private final Routes<SingleDestination<?>> routes;
	private final Map<NamedType<?>, List<SingleDestination<?>>> availableDestinations;
	private static final List<TransitionResolver> transitionResolvers = Arrays.asList(new StartResolver(), new BridgeResolver());

	public InitLikeStateMachine(Routes<SingleDestination<?>> routes, Map<NamedType<?>, List<SingleDestination<?>>> availableDestinations) {
		this.routes = routes;
		this.availableDestinations = availableDestinations;
	}
	
	public <T> State<T> init(NamedType<T> type) {
		List<SingleDestination<?>> possibleRoutes = Objects.requireNonNull(availableDestinations.get(type),() -> "no route to "+type+" found");
		if (possibleRoutes.size()>1) {
			throw new IllegalArgumentException("there are more than one way to get here: "+type);
		}
		SingleDestination<T> route = (SingleDestination<T>) possibleRoutes.get(0);
		
		for (TransitionResolver resolver : transitionResolvers) {
			Optional<Function<StateResolver, State<T>>> resolvedTransition = resolver.resolve(route, routes.transitionOf(route));
			if (resolvedTransition.isPresent()) {
				return resolvedTransition.get().apply(new StateResolver() {
					
					@Override
					public <D> State<D> resolve(NamedType<D> type) {
						return init(type);
					}
				});
			}
		}
		
		throw new IllegalArgumentException("could not resolve: "+type);
	}
	
	public static InitLikeStateMachine with(Routes<SingleDestination<?>> routes) {
		UnmodifiableDirectedGraph<NamedType<?>, DefaultEdge> routesAsGraph=asGraph(routes.all());
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

	private static UnmodifiableDirectedGraph<NamedType<?>, DefaultEdge> asGraph(Set<SingleDestination<?>> all) {
		return new UnmodifiableDirectedGraph<>(Graphs.with(Graphs.<NamedType<?>>directedGraphBuilder()).build(graph -> {
			all.forEach(r -> {
				graph.addVertex(r.destination());
				r.sources().forEach(s -> {
					graph.addVertex(s);
					graph.addEdge(s, r.destination());
				});
			});
		}));
	}
	 
}
