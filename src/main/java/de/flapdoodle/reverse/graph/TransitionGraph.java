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
import de.flapdoodle.graph.GraphBuilder;
import de.flapdoodle.graph.Loop;
import de.flapdoodle.reflection.ClassTypeInfo;
import de.flapdoodle.reflection.TypeInfo;
import de.flapdoodle.reverse.StateID;
import de.flapdoodle.reverse.Transition;
import de.flapdoodle.reverse.Transitions;
import de.flapdoodle.reverse.types.TypeNames;
import de.flapdoodle.types.Either;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class TransitionGraph {
	private static final String JAVA_LANG_PACKAGE = "java.lang.";
	
	public static String edgeGraphAsDot(String label, Transitions transitions) {
		return edgeGraphAsDot(label, asGraph(transitions.transitions()));
	}

	public static DefaultDirectedGraph<Vertex, DefaultEdge> asGraph(List<? extends Transition<?>> all) {
		GraphBuilder<Vertex, DefaultEdge, DefaultDirectedGraph<Vertex, DefaultEdge>> builder = GraphBuilder.withDirectedGraph();
		all.forEach(edge -> {
				StateVertex destination = StateVertex.of(edge.destination());
				TransitionVertex transition = TransitionVertex.of(edge);

				builder.addVertex(destination);
				builder.addVertex(transition);
				builder.addEdge(transition, destination);
				edge.sources().forEach(source -> {
					StateVertex s = StateVertex.of(source);
					builder.addVertex(s);
					builder.addEdge(s, transition);
				});
		});
		return builder.build();
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
			.sortedBy(vertexAsLabel::apply)
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

	public static String asMessage(List<? extends Loop<Vertex, DefaultEdge>> loops) {
		return loops.stream().map(TransitionGraph::asMessage).reduce((l, r) -> l + "\n" + r).orElse("");
	}

	private static String asMessage(Loop<Vertex, DefaultEdge> loop) {
		return loop.vertexSet().stream()
			.map(TransitionGraph::asMessage)
			.reduce((l, r) -> l + "->" + r)
			.get();
	}

	public static String asMessage(Collection<StateID<?>> types) {
		return types.stream()
			.map(TransitionGraph::asMessage)
			.collect(Collectors.joining(", "));
	}

	private static String asMessage(Vertex type) {
		return Vertex.asEither(type)
			.mapLeft(StateVertex::stateId)
			.mapLeft(TransitionGraph::asMessage)
			.mapRight(TransitionVertex::transition)
			.mapRight(TransitionGraph::asMessage)
			.map(Function.identity(), Function.identity());
	}

	public static String asMessage(StateID<?> type) {
		return "State(" + (type.name().isEmpty() ? typeAsMessage(type.type()) : type.name() + ":" + typeAsMessage(type.type())) + ")";
	}

	private static String asMessage(Transition<?> transition) {
		return transition.toString();
	}
	
	private static String typeAsMessage(TypeInfo<?> typeInfo) {
		return TypeNames.typeName(typeInfo);
	}
}
