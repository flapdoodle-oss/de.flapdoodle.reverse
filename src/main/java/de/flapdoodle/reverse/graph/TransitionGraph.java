/*
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
package de.flapdoodle.reverse.graph;

import de.flapdoodle.graph.GraphAsDot;
import de.flapdoodle.graph.Graphs;
import de.flapdoodle.reverse.StateID;
import de.flapdoodle.reverse.Transition;
import de.flapdoodle.reverse.Transitions;
import de.flapdoodle.types.Either;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class TransitionGraph {
	public static String edgeGraphAsDot(String label, Transitions transitions) {
		return edgeGraphAsDot(label, asGraph(transitions.transitions()));
	}

	public static DefaultDirectedGraph<Vertex, DefaultEdge> asGraph(List<? extends Transition<?>> all) {
		Supplier<Graphs.GraphBuilder<Vertex, DefaultEdge, DefaultDirectedGraph<Vertex, DefaultEdge>>> directedGraph = Graphs
			.graphBuilder(Graphs.directedGraph(DefaultEdge.class));
		return Graphs.with(directedGraph).build(graph -> all.forEach(edge -> {
			StateVertex destination = StateVertex.of(edge.destination());
			TransitionVertex transition = TransitionVertex.of(edge);

			graph.addVertex(destination);
			graph.addVertex(transition);
			graph.addEdge(transition, destination);
			edge.sources().forEach(source -> {
				StateVertex s = StateVertex.of(source);
				graph.addVertex(s);
				graph.addEdge(s, transition);
			});
		}));
	}

	public static String edgeGraphAsDot(String label, DefaultDirectedGraph<Vertex, DefaultEdge> graph) {
		return edgeGraphAsDot(label, graph, Transition::asLabel, StateID::asLabel);
	}

	public static String edgeGraphAsDot(String label, DefaultDirectedGraph<Vertex, DefaultEdge> graph,
		Function<Transition<?>, String> transitionAsLabel,
		Function<StateID<?>, String> stateIdAsLabel
	) {
		Function<Vertex, String> vertexAsLabel = t -> {
			Either<StateVertex, TransitionVertex> stateOrTransition = Vertex.asEither(t);

			return stateOrTransition
				.mapLeft(StateVertex::stateId)
				.mapLeft(stateIdAsLabel::apply)
				.mapRight(TransitionVertex::transition)
				.mapRight(transitionAsLabel::apply)
				.map(Function.identity(), Function.identity());
		};

		return GraphAsDot.builder(Vertex.asId())
			.subGraphIdSeparator("__")
			.label(label)
			.nodeAsLabel(vertexAsLabel)
			.sortedBy((GraphAsDot.AsComparable<Vertex, String>) vertexAsLabel::apply)
			.nodeAttributes(t -> {
				Either<StateVertex, TransitionVertex> stateOrTransition = Vertex.asEither(t);
				String shape = stateOrTransition.isLeft() ? "ellipse" : "rectangle";
				return asMap("shape", shape);
			})
			.subGraph(t -> {
				if (t instanceof TransitionVertex) {
					Transition<?> transition = ((TransitionVertex) t).transition();
					if (transition instanceof HasSubGraph) {
						return Optional.of(((HasSubGraph) transition).subGraph());
					}
				}
				return Optional.empty();
			})
			.build().asDot(graph);
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
}
