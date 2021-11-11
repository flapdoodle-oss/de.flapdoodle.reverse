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
package de.flapdoodle.reverse.edges;

import de.flapdoodle.reverse.StateID;
import de.flapdoodle.reverse.Transition;
import de.flapdoodle.reverse.State;
import de.flapdoodle.reverse.StateLookup;
import org.immutables.value.Value;

import java.util.Collections;
import java.util.Set;
import java.util.function.Function;

@Value.Immutable
public abstract class Derive<S, D> implements Transition<D> {
		public abstract StateID<S> source();

		public abstract StateID<D> destination();

		protected abstract Function<S, State<D>> action();

		@Override
		@Value.Lazy
		public Set<StateID<?>> sources() {
				return Collections.singleton(source());
		}

		@Override
		@Value.Auxiliary
		public State<D> result(StateLookup lookup) {
				return action().apply(lookup.of(source()));
		}



		public static <S, D> Derive<S, D> of(StateID<S> source, StateID<D> dest, Function<S, State<D>> action) {
				return ImmutableDerive.<S, D>builder()
						.source(source)
						.destination(dest)
						.action(action)
						.build();
		}

		public static <D> WithSource<D> given(StateID<D> source) {
				return new WithSource<D>(source);
		}

		public static <D> WithSource<D> given(Class<D> sourceType) {
				return given(StateID.of(sourceType));
		}

		public static class WithSource<S> {
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

		public static class WithSourceAndDestination<S, D> {
				private final StateID<S> source;
				private final StateID<D> destination;

				public WithSourceAndDestination(StateID<S> source, StateID<D> destination) {
						this.source = source;
						this.destination = destination;
				}

				public Derive<S,D> deriveBy(Function<S,D> action) {
						return with(action.andThen(State::of));
				}

				public Derive<S,D> with(Function<S,State<D>> action) {
						return Derive.of(source,destination,action);
				}
		}
}
