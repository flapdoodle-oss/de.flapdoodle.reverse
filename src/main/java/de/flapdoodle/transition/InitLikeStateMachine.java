package de.flapdoodle.transition;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.UnmodifiableDirectedGraph;

import de.flapdoodle.graph.Graphs;
import de.flapdoodle.graph.Loop;
import de.flapdoodle.transition.routes.Route;
import de.flapdoodle.transition.routes.Route.Transition;
import de.flapdoodle.transition.routes.Routes;
import de.flapdoodle.transition.routes.Start;

public class InitLikeStateMachine {

	private final Routes routes;
	private final Map<NamedType<?>, List<Route<?>>> availableDestinations;

	public InitLikeStateMachine(Routes routes, Map<NamedType<?>, List<Route<?>>> availableDestinations) {
		this.routes = routes;
		this.availableDestinations = availableDestinations;
	}
	
	public <T> State<T> init(NamedType<T> type) {
		List<Route<?>> possibleRoutes = Objects.requireNonNull(availableDestinations.get(type),() -> "no route to "+type+" found");
		if (possibleRoutes.size()>1) {
			throw new IllegalArgumentException("there are more than one way to get here: "+type);
		}
		Route<?> route = possibleRoutes.get(0);
		
		Transition<?> transition = routes.transitionOf(route);
		if (transition instanceof Start.Transition) {
			Start.Transition<T> start = (Start.Transition<T>) transition;
			return start.get();
		}
		
		throw new IllegalArgumentException("not implemented");
	}

	public static InitLikeStateMachine with(Routes routes) {
		UnmodifiableDirectedGraph<NamedType<?>, DefaultEdge> routesAsGraph=asGraph(routes.all());
		List<? extends Loop<NamedType<?>, DefaultEdge>> loops = Graphs.loopsOf(routesAsGraph);
		
		if (!loops.isEmpty()) {
			throw new IllegalArgumentException("loops are not supported: "+asMessage(loops));
		}
		
		Map<NamedType<?>, List<Route<?>>> availableDestinations = routes.all().stream()
			.collect(Collectors.groupingBy(r -> r.destination()));
		
		return new InitLikeStateMachine(routes, availableDestinations);
	}

	private static String asMessage(List<? extends Loop<NamedType<?>, DefaultEdge>> loops) {
		return loops.stream().map(l -> asMessage(l)).reduce((l,r) -> l+"\n"+r).orElse("");
	}

	private static String asMessage(Loop<NamedType<?>, DefaultEdge> loop) {
		return loop.vertexSet().stream().map(v -> v.toString()).reduce((l,r) -> l+"->"+r).get();
	}

	private static UnmodifiableDirectedGraph<NamedType<?>, DefaultEdge> asGraph(Set<Route<?>> all) {
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
