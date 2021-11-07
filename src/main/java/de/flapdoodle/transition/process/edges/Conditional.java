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
package de.flapdoodle.transition.process.edges;

import de.flapdoodle.transition.StateID;
import de.flapdoodle.transition.process.Edge;
import de.flapdoodle.transition.process.HasSource;
import de.flapdoodle.types.Either;
import org.immutables.value.Value;

import java.util.function.Function;

@Value.Immutable
public interface Conditional<S,D1,D2> extends Edge, HasSource<S> {
		@Override
		@Value.Parameter
		StateID<S> source();

		@Value.Parameter
		StateID<D1> firstDestination();

		@Value.Parameter
		StateID<D2> secondDestination();

		@Value.Parameter
		Function<S, Either<D1, D2>> action();

		static <S,D1, D2> Conditional<S,D1,D2> of(StateID<S> source, StateID<D1> first, StateID<D2> second, Function<S, Either<D1,D2>> action) {
				return ImmutableConditional.of(source,first,second,action);
		}
}
