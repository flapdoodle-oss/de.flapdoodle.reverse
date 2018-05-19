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
package de.flapdoodle.transition.routes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.immutables.value.Value;
import org.immutables.value.Value.Parameter;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.UnmodifiableDirectedGraph;

import de.flapdoodle.graph.GraphAsDot;
import de.flapdoodle.graph.Graphs;
import de.flapdoodle.graph.Graphs.GraphBuilder;
import de.flapdoodle.transition.NamedType;

public abstract class RoutesAsGraph {

	public static UnmodifiableDirectedGraph<NamedType<?>, RouteAndVertex> asGraph(Set<? extends Route<?>> all) {
		return asGraph(all, false);
	}

	public static UnmodifiableDirectedGraph<NamedType<?>, RouteAndVertex> asGraphIncludingStartAndEnd(Set<? extends Route<?>> all) {
		return asGraph(all, true);
	}

	private static UnmodifiableDirectedGraph<NamedType<?>, RouteAndVertex> asGraph(Set<? extends Route<?>> all, boolean addEmptyVertex) {
		Supplier<GraphBuilder<NamedType<?>, RouteAndVertex, DefaultDirectedGraph<NamedType<?>, RouteAndVertex>>> directedGraph = Graphs
				.graphBuilder(Graphs.directedGraph(RouteAndVertex.class));
		return new UnmodifiableDirectedGraph<>(Graphs.with(directedGraph).build(graph -> {
			AtomicInteger voidCounter=new AtomicInteger();

			all.forEach(r -> {
				if (r instanceof SingleDestination<?>) {
					SingleDestination<?> s = (SingleDestination<?>) r;
					graph.addVertex(s.destination());
					s.sources().forEach(source -> {
						graph.addVertex(source);
						graph.addEdge(source, s.destination(), RouteAndVertex.of(source, s, s.destination()));
					});
					if (addEmptyVertex && (r instanceof Start)) {
						NamedType<Void> start=NamedType.typeOf("start_"+voidCounter.incrementAndGet(), Void.class);
						graph.addVertex(start);
						graph.addEdge(start, s.destination(), RouteAndVertex.of(start, s, s.destination()));
					}
				} else {
					if (r instanceof PartingWay) {
						PartingWay<?, ?, ?> s = (PartingWay<?, ?, ?>) r;
						graph.addVertex(s.start());
						graph.addVertex(s.oneDestination());
						graph.addVertex(s.otherDestination());
						graph.addEdge(s.start(), s.oneDestination(), RouteAndVertex.of(s.start(), s, s.oneDestination()));
						graph.addEdge(s.start(), s.otherDestination(), RouteAndVertex.of(s.start(), s, s.otherDestination()));
					} else {
						if (addEmptyVertex  && (r instanceof End)) {
							End<?> s = (End<?>) r;
							NamedType<Void> end=NamedType.typeOf("end_"+voidCounter.incrementAndGet(), Void.class);
							graph.addVertex(end);
							graph.addEdge(s.start(), end, RouteAndVertex.of(s.start(), s, end));
						}
						else throw new IllegalArgumentException("unknown route type: " + r);
					}
				}
			});
		}));
	}

	public static String routeGraphAsDot(String label, DirectedGraph<NamedType<?>, RouteAndVertex> graph) {
		return GraphAsDot.builder(RoutesAsGraph::asLabel)
				.label(label)
				.edgeAttributes((a, b) -> {
					Route<?> route = graph.getEdge(a, b).route();
					String routeLabel = routeAsLabel(route);
					return asMap("label", routeLabel);
				})
				.nodeAttributes(t -> {
					if (t.type()==Void.class) {
						return asMap("shape", "circle","label", "");
					}
					String nodeLabel = asHumanReadableLabel(t);
					return asMap("shape", "rectangle","label", nodeLabel);
				})
				.build().asDot(graph);
	}

	private static String asHumanReadableLabel(NamedType<?> t) {
		String nodeLabel = t.name()+":"+t.type().getTypeName();
		if (t.type() instanceof Class) {
			nodeLabel = t.name()+":"+((Class) t.type()).getSimpleName();
		}
		return nodeLabel;
	}

	private static String routeAsLabel(Route<?> route) {
		if (route instanceof Start) {
			return Start.class.getSimpleName();
		}
		if (route instanceof End) {
			return End.class.getSimpleName();
		}
		if (route instanceof Bridge) {
			return Bridge.class.getSimpleName();
		}
		if (route instanceof MergingJunction) {
			return MergingJunction.class.getSimpleName();
		}
		if (route instanceof ThreeWayMergingJunction) {
			return ThreeWayMergingJunction.class.getSimpleName();
		}
		if (route instanceof PartingWay) {
			return PartingWay.class.getSimpleName();
		}
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

	private static String asLabel(NamedType<?> type) {
		return (type.name().isEmpty() ? "<empty>" : type.name()) + ":" + type.type().toString();
	}

	@Value.Immutable
	public interface RouteAndVertex {
		@Parameter
		NamedType<?> start();

		@Parameter
		Route<?> route();

		@Parameter
		NamedType<?> end();

		public static RouteAndVertex of(NamedType<?> start, Route<?> route, NamedType<?> end) {
			return ImmutableRouteAndVertex.of(start, route, end);
		}
	}
}
