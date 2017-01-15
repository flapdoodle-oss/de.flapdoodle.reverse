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
		Supplier<GraphBuilder<NamedType<?>, RouteAndVertex, DefaultDirectedGraph<NamedType<?>, RouteAndVertex>>> directedGraph = Graphs.graphBuilder(Graphs.directedGraph(RouteAndVertex.class));
		return new UnmodifiableDirectedGraph<>(Graphs.with(directedGraph).build(graph -> {
			all.forEach(r -> {
				if (r instanceof SingleDestination<?>) {
					SingleDestination<?> s=(SingleDestination<?>) r;
					graph.addVertex(s.destination());
					s.sources().forEach(source -> {
						graph.addVertex(source);
						graph.addEdge(source, s.destination(),RouteAndVertex.of(source, s, s.destination()));
					});
				} else {
					if (r instanceof PartingWay) {
						PartingWay<?,?,?> s=(PartingWay<?,?,?>) r;
						graph.addVertex(s.start());
						graph.addVertex(s.oneDestination());
						graph.addVertex(s.otherDestination());
						graph.addEdge(s.start(), s.oneDestination(),RouteAndVertex.of(s.start(), s, s.oneDestination()));
						graph.addEdge(s.start(), s.otherDestination(),RouteAndVertex.of(s.start(), s, s.otherDestination()));
					} else {
						throw new IllegalArgumentException("unknown route type: "+r);
					}
				}
			});
		}));
	}
	
	public static String routeGraphAsDot(String label, DirectedGraph<NamedType<?>, RouteAndVertex> graph) {
		return GraphAsDot.builder(RoutesAsGraph::asLabel)
			.label(label)
			.edgeAttributes((a,b) -> {
				String routeLabel = graph.getEdge(a, b).route().getClass().getSimpleName();
				return asMap("label",routeLabel);
			})
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
		return (type.name().isEmpty() ? "<empty>" : type.name())+":"+type.type().getName();
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
