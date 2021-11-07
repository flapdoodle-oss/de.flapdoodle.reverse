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
package de.flapdoodle.transition.processlike;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import de.flapdoodle.transition.processlike.edges.Conditional;
import de.flapdoodle.transition.processlike.edges.Start;
import de.flapdoodle.transition.processlike.edges.Step;
import de.flapdoodle.transition.processlike.edges.End;
import org.immutables.value.Value;
import org.immutables.value.Value.Parameter;

import de.flapdoodle.graph.GraphAsDot;
import de.flapdoodle.graph.Graphs;
import de.flapdoodle.graph.Graphs.GraphBuilder;
import de.flapdoodle.transition.StateID;
import org.jgrapht.graph.DefaultDirectedGraph;

public abstract class RoutesAsGraph {

	public static DefaultDirectedGraph<StateID<?>, RouteAndVertex> asGraph(List<? extends Edge> all) {
		return asGraph(all, false);
	}

	public static DefaultDirectedGraph<StateID<?>, RouteAndVertex>
			asGraphIncludingStartAndEnd(List<? extends Edge> all) {
		return asGraph(all, true);
	}

	private static DefaultDirectedGraph<StateID<?>, RouteAndVertex> asGraph(List<? extends Edge> all,
			boolean addEmptyVertex) {
		Supplier<GraphBuilder<StateID<?>, RouteAndVertex, DefaultDirectedGraph<StateID<?>, RouteAndVertex>>> directedGraph = Graphs
				.graphBuilder(Graphs.directedGraph(RouteAndVertex.class));
			return Graphs.with(directedGraph).build(graph -> {
					AtomicInteger voidCounter = new AtomicInteger();

					all.forEach(r -> {
							if (addEmptyVertex && r instanceof Start) {
									Start<?> start = (Start<?>) r;
									StateID<Void> startID = StateID.of("start_" + voidCounter.incrementAndGet(), Void.class);
									graph.addVertex(startID);
									graph.addVertex(start.destination());
									graph.addEdge(startID, start.destination(), RouteAndVertex.of(startID, r, start.destination()));
							}
							if (r instanceof Step) {
									Step<?, ?> step = (Step<?, ?>) r;
									graph.addVertex(step.source());
									graph.addVertex(step.destination());
									graph.addEdge(step.source(), step.destination(), RouteAndVertex.of(step.source(), r, step.destination()));
							}
							if (r instanceof Conditional) {
									Conditional<?, ?, ?> conditional = (Conditional<?, ?, ?>) r;
									graph.addVertex(conditional.source());
									graph.addVertex(conditional.firstDestination());
									graph.addVertex(conditional.secondDestination());
									graph.addEdge(conditional.source(), conditional.firstDestination(), RouteAndVertex.of(conditional.source(), conditional, conditional.firstDestination()));
									graph.addEdge(conditional.source(), conditional.secondDestination(), RouteAndVertex.of(conditional.source(), conditional, conditional.secondDestination()));
							}
							if (addEmptyVertex && r instanceof End) {
									End<?> s = (End<?>) r;
									StateID<Void> end = StateID.of("end_" + voidCounter.incrementAndGet(), Void.class);
									graph.addVertex(end);
									graph.addEdge(s.source(), end, RouteAndVertex.of(s.source(), s, end));
							}
					});
			});
	}

	public static String routeGraphAsDot(String label, DefaultDirectedGraph<StateID<?>, RouteAndVertex> graph) {
		return routeGraphAsDot(label, graph, RoutesAsGraph::routeAsLabel);
	}

	public static String routeGraphAsDot(String label, DefaultDirectedGraph<StateID<?>, RouteAndVertex> graph,
			Function<Edge, String> routeAsLabel) {
		return GraphAsDot.builder(RoutesAsGraph::asLabel)
				.label(label)
				.edgeAttributes((a, b) -> {
						Edge route = graph.getEdge(a, b).route();
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
		String nodeLabel = t.name() + ":" + t.type().getTypeName();
		if (t.type() instanceof Class) {
			nodeLabel = t.name() + ":" + ((Class) t.type()).getSimpleName();
		}
		return nodeLabel;
	}

	private static String routeAsLabel(Edge route) {
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
	public interface RouteAndVertex {
		@Parameter
		StateID<?> start();

		@Parameter
		Edge route();

		@Parameter
		StateID<?> end();

		public static RouteAndVertex of(StateID<?> start, Edge route, StateID<?> end) {
			return ImmutableRouteAndVertex.of(start, route, end);
		}
	}
}
