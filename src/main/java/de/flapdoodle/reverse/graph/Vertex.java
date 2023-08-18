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

import de.flapdoodle.reverse.Transition;
import de.flapdoodle.types.Either;

import java.util.LinkedHashMap;
import java.util.function.Function;

public abstract class Vertex {

	public static Function<Vertex, String> asId() {
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
		throw new IllegalArgumentException("unknown vertex type: " + vertex + "(" + vertex.getClass() + ")");
	}
}
