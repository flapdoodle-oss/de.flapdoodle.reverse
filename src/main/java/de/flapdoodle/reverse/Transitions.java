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
package de.flapdoodle.reverse;

import de.flapdoodle.graph.GraphAsDot;
import de.flapdoodle.graph.Graphs;
import de.flapdoodle.graph.ImmutableSubGraph;
import de.flapdoodle.reverse.naming.HasLabel;
import de.flapdoodle.reverse.types.TypeNames;
import de.flapdoodle.types.Either;
import org.immutables.value.Value;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class Transitions {

	public static DefaultDirectedGraph<Vertex, DefaultEdge> asGraph(List<? extends Transition<?>> all) {
		Supplier<Graphs.GraphBuilder<Vertex, DefaultEdge, DefaultDirectedGraph<Vertex, DefaultEdge>>> directedGraph = Graphs
			.graphBuilder(Graphs.directedGraph(DefaultEdge.class));
		return Graphs.with(directedGraph).build(graph -> {
			all.forEach(edge -> {
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
			});
		});
	}

	public static String edgeGraphAsDot(String label, DefaultDirectedGraph<Vertex, DefaultEdge> graph) {
		return edgeGraphAsDot(label, graph, Transitions::transitionAsLabel, Transitions::stateAsLabel);
	}

	public static String edgeGraphAsDot(String label, DefaultDirectedGraph<Vertex, DefaultEdge> graph,
		Function<Transition<?>, String> transitionAsLabel,
		Function<StateID<?>, String> stateIdAsLabel
	) {
		return GraphAsDot.builder(Transitions.asId())
			.subGraphIdSeparator("__")
			.label(label)
			.nodeAsLabel(t -> {
				Either<StateVertex, TransitionVertex> stateOrTransition = asEither(t);

				return stateOrTransition
					.mapLeft(StateVertex::stateId)
					.mapLeft(stateIdAsLabel::apply)
					.mapRight(TransitionVertex::transition)
					.mapRight(transitionAsLabel::apply)
					.map(Function.identity(), Function.identity());
			})
			.nodeAttributes(t -> {
				Either<StateVertex, TransitionVertex> stateOrTransition = asEither(t);
				String shape = stateOrTransition.isLeft() ? "ellipse" : "rectangle";
				return asMap("shape", shape);
			})
			.subGraph(t -> {
				if (t instanceof TransitionVertex) {
					Transition<?> transition = ((TransitionVertex) t).transition();
					if (transition instanceof TransitionWalker.MappedWrapper) {
						TransitionWalker.MappedWrapper<?> wrapper = (TransitionWalker.MappedWrapper<?>) transition;
						ImmutableSubGraph<Vertex> subGraph = GraphAsDot.SubGraph.of(wrapper.graph())
							.connections(asSubGraphMap(wrapper.transitionMapping(), wrapper.missingSources()))
							.build();

						return Optional.of(subGraph);
					}
				}
				return Optional.empty();
			})
			.build().asDot(graph);
	}

	private static Map<? extends Vertex, ? extends Vertex> asSubGraphMap(TransitionMapping<?> transitionMapping, Set<StateID<?>> missingSources) {
		Map<Vertex, Vertex> ret=new LinkedHashMap<>();
		missingSources.forEach(dest -> {
			StateID<?> source = transitionMapping.sourceOf(dest);
			ret.put(StateVertex.of(source), StateVertex.of(dest));
		});
		ret.put(StateVertex.of(transitionMapping.destination().destination()), StateVertex.of(transitionMapping.destination().source()));

		return Collections.unmodifiableMap(ret);
	}

	private static String stateAsLabel(StateID<?> t) {
		return (t.name().isEmpty() ? "<empty>" : t.name()) + ":" + TypeNames.typeName(t.type());
	}

	private static String transitionAsLabel(Transition<?> route) {
		if (route instanceof HasLabel) return ((HasLabel) route).transitionLabel();
		return TypeNames.typeName(route.getClass());
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

	private static Function<Vertex, String> asId() {
		LinkedHashMap<Class<?>, Integer> typeCounter=new LinkedHashMap<>();
		LinkedHashMap<Transition<?>, Integer> transitionMap = new LinkedHashMap<>();

		return vertex -> asEither(vertex)
			.mapLeft(StateVertex::stateId)
			.mapLeft(type -> (type.name().isEmpty() ? "<empty>" : type.name()) + ":" + type.type().toString())
			.mapRight(TransitionVertex::transition)
			.mapRight(transition -> {
				Integer number= transitionMap.get(transition);
				if (number==null) {
					typeCounter.putIfAbsent(transition.getClass(),-1);
					number = typeCounter.get(transition.getClass()) + 1;
					typeCounter.put(transition.getClass(), number);
					transitionMap.put(transition, number);
				}
				return transition.getClass().getName() +":"+ number;
			})
			.map(Function.identity(), Function.identity());
	}

	public static Either<StateVertex, TransitionVertex> asEither(Vertex vertex) {
		if (vertex instanceof StateVertex) return Either.left((StateVertex) vertex);
		if (vertex instanceof TransitionVertex) return Either.right((TransitionVertex) vertex);
		throw new IllegalArgumentException("unknown vertext type: " + vertex + "(" + vertex.getClass() + ")");
	}

	interface Vertex {

	}

	@Value.Immutable
	interface StateVertex extends Vertex {
		@Value.Parameter
		StateID<?> stateId();

		static StateVertex of(StateID<?> stateId) {
			return ImmutableStateVertex.of(stateId);
		}
	}

	@Value.Immutable
	interface TransitionVertex extends Vertex {
		@Value.Parameter
		Transition<?> transition();

		static TransitionVertex of(Transition<?> transition) {
			return ImmutableTransitionVertex.of(transition);
		}
	}
}
