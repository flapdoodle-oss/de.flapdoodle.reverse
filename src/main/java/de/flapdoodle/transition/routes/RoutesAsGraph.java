package de.flapdoodle.transition.routes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.UnmodifiableDirectedGraph;

import de.flapdoodle.graph.GraphAsDot;
import de.flapdoodle.graph.Graphs;
import de.flapdoodle.graph.Graphs.GraphBuilder;
import de.flapdoodle.transition.NamedType;

public abstract class RoutesAsGraph {

	public static UnmodifiableDirectedGraph<NamedType<?>, Route<?>> asGraph(Set<? extends Route<?>> all) {
		Supplier<GraphBuilder<NamedType<?>, Route<?>, DefaultDirectedGraph<NamedType<?>, Route<?>>>> directedGraph = Graphs.graphBuilder(Graphs.directedGraph(Route.class));
		return new UnmodifiableDirectedGraph<>(Graphs.with(directedGraph).build(graph -> {
			all.forEach(r -> {
				if (r instanceof SingleDestination<?>) {
					SingleDestination<?> s=(SingleDestination<?>) r;
					graph.addVertex(s.destination());
					s.sources().forEach(source -> {
						graph.addVertex(source);
						graph.addEdge(source, s.destination());
					});
				} else {
					if (r instanceof PartingWay) {
						PartingWay<?,?,?> s=(PartingWay<?,?,?>) r;
						graph.addVertex(s.start());
						graph.addVertex(s.oneDestination());
						graph.addVertex(s.otherDestination());
						graph.addEdge(s.start(), s.oneDestination());
						graph.addEdge(s.start(), s.otherDestination());
					} else {
						throw new IllegalArgumentException("unknown route type: "+r);
					}
				}
			});
		}));
	}
	
	public static String routeGraphAsDot(String label, DirectedGraph<NamedType<?>, ?> graph) {
		return GraphAsDot.builder(RoutesAsGraph::asLabel)
			.label(label)
			.nodeAttributes(t -> asMap("shape","rectangle"))
			.build().asDot(graph);
	}
	
	private static Map<String, String> asMap(String...keyValues) {
		LinkedHashMap<String, String> ret = new LinkedHashMap<>();
		if ((keyValues.length % 2) != 0) {
			throw new IllegalArgumentException("parameter not modulo of 2");
		}
		for (int i=0;i<keyValues.length;i=i+2) {
			ret.put(keyValues[i], keyValues[i+1]);
		}
		return ret;
	}

	private static String asLabel(NamedType<?> type) {
		return type.name()+" : "+type.type().getName();
	}
}
