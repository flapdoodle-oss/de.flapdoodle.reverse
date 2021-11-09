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
package de.flapdoodle.transition.initlike.edges;

import de.flapdoodle.transition.StateID;
import de.flapdoodle.transition.initlike.Edge;
import de.flapdoodle.transition.initlike.State;
import org.immutables.value.Value;

import java.util.function.Function;
import java.util.function.Supplier;

@Value.Immutable
public interface Depends<S, D> extends Edge<D> {
		StateID<S> source();

		StateID<D> destination();

		Function<S, State<D>> action();

		static <S, D> Depends<S, D> of(StateID<S> source, StateID<D> dest, Function<S, State<D>> action) {
				return ImmutableDepends.<S, D>builder()
						.source(source)
						.destination(dest)
						.action(action)
						.build();
		}

		static <D> WithSource<D> given(StateID<D> source) {
				return new WithSource<D>(source);
		}

		static <D> WithSource<D> given(Class<D> sourceType) {
				return given(StateID.of(sourceType));
		}

		class WithSource<S> {
				private final StateID<S> source;
				private WithSource(StateID<S> source) {
						this.source = source;
				}

				public <D> WithSourceAndDestination<S, D> state(StateID<D> destination) {
						return new WithSourceAndDestination<>(source, destination);
				}

				public <D> WithSourceAndDestination<S, D> state(Class<D> destination) {
						return state(StateID.of(destination));
				}
		}

		class WithSourceAndDestination<S, D> {
				private final StateID<S> source;
				private final StateID<D> destination;

				public WithSourceAndDestination(StateID<S> source, StateID<D> destination) {
						this.source = source;
						this.destination = destination;
				}

				public Depends<S,D> deriveBy(Function<S,D> action) {
						return with(action.andThen(State::of));
				}

				public Depends<S,D> with(Function<S,State<D>> action) {
						return Depends.of(source,destination,action);
				}
		}
}
