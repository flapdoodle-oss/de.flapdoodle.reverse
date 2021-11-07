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

import de.flapdoodle.graph.GraphAsDot;
import de.flapdoodle.graph.Graphs;
import de.flapdoodle.graph.Graphs.GraphBuilder;
import de.flapdoodle.transition.StateID;
import de.flapdoodle.transition.initlike.edges.Depends;
import de.flapdoodle.transition.initlike.edges.Merge2;
import de.flapdoodle.transition.initlike.edges.Merge3;
import de.flapdoodle.transition.initlike.edges.Start;
import org.immutables.value.Value;
import org.immutables.value.Value.Parameter;
import org.jgrapht.graph.DefaultDirectedGraph;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class EdgesAsGraph {

	public static DefaultDirectedGraph<StateID<?>, EdgeAndVertex> asGraph(List<? extends Edge<?>> all) {
		return asGraph(all, false);
	}

	public static DefaultDirectedGraph<StateID<?>, EdgeAndVertex>
			asGraphIncludingStartAndEnd(List<? extends Edge<?>> all) {
		return asGraph(all, true);
	}

	private static DefaultDirectedGraph<StateID<?>, EdgeAndVertex> asGraph(List<? extends Edge<?>> all,
			boolean addEmptyVertex) {
		Supplier<GraphBuilder<StateID<?>, EdgeAndVertex, DefaultDirectedGraph<StateID<?>, EdgeAndVertex>>> directedGraph = Graphs
				.graphBuilder(Graphs.directedGraph(EdgeAndVertex.class));
			return Graphs.with(directedGraph).build(graph -> {
					AtomicInteger voidCounter = new AtomicInteger();

					all.forEach(edge -> {
							graph.addVertex(edge.destination());
							Edges.sources(edge).forEach(source -> {
									graph.addVertex(source);
									graph.addEdge(source, edge.destination(), EdgeAndVertex.of(source, edge, edge.destination()));
							});
							if (addEmptyVertex && (edge instanceof de.flapdoodle.transition.initlike.edges.Start)) {
									StateID<Void> start = StateID.of("start_" + voidCounter.incrementAndGet(), Void.class);
									graph.addVertex(start);
									graph.addEdge(start, edge.destination(), EdgeAndVertex.of(start, edge, edge.destination()));
							}
					});
			});
	}

	public static String edgeGraphAsDot(String label, DefaultDirectedGraph<StateID<?>, EdgeAndVertex> graph) {
		return edgeGraphAsDot(label, graph, EdgesAsGraph::routeAsLabel);
	}

	public static String edgeGraphAsDot(String label, DefaultDirectedGraph<StateID<?>, EdgeAndVertex> graph,
			Function<Edge<?>, String> routeAsLabel) {
		return GraphAsDot.builder(EdgesAsGraph::asLabel)
				.label(label)
				.edgeAttributes((a, b) -> {
					Edge<?> route = graph.getEdge(a, b).edge();
					String routeLabel = routeAsLabel.apply(route);
					return asMap("label", routeLabel);
				})
				.nodeAttributes(t -> {
					if (t.type() == Void.class) {
						return asMap("shape", "circle", "label", "");
					}
					String nodeLabel = asHumanReadableLabel(t);
					return asMap("shape", "rectangle", "label", nodeLabel);
				})
				.build().asDot(graph);
	}

	private static String asHumanReadableLabel(StateID<?> t) {
		return t.name() + ":" + t.type().getSimpleName();
	}

	private static String routeAsLabel(Edge<?> route) {
			if (route instanceof Start) return Start.class.getSimpleName();
			if (route instanceof Depends) return Depends.class.getSimpleName();
			if (route instanceof Merge2) return Merge2.class.getSimpleName();
			if (route instanceof Merge3) return Merge3.class.getSimpleName();
		return route.getClass().getSimpleName();
	}

	private static Map<String, String> asMap(String... keyValues) {
		LinkedHashMap<String, String> ret = new LinkedHashMap<>();
		if ((keyValues.length % 2) != 0) {
			throw new IllegalArgumentException("parameter not modulo of 2");
		}
		for (int i = 0; i < keyValues.length; i = i + 2) {
			ret.put(keyValues[i], keyValues[i + 1]);
		}
		return ret;
	}

	private static String asLabel(StateID<?> type) {
		return (type.name().isEmpty() ? "<empty>" : type.name()) + ":" + type.type().toString();
	}

	@Value.Immutable
	public interface EdgeAndVertex {
		@Parameter
		StateID<?> start();

		@Parameter
		Edge<?> edge();

		@Parameter
		StateID<?> end();

		public static EdgeAndVertex of(StateID<?> start, Edge<?> route, StateID<?> end) {
			return ImmutableEdgeAndVertex.of(start, route, end);
		}
	}
}
