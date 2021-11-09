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

import java.util.function.BiFunction;

@Value.Immutable
public interface Merge2<L, R, D> extends Edge<D> {
		StateID<L> left();

		StateID<R> right();

		StateID<D> destination();

		BiFunction<L, R, State<D>> action();

		static <L, R, D> Merge2<L, R, D> of(StateID<L> left, StateID<R> right, StateID<D> dest, BiFunction<L, R, State<D>> action) {
				return ImmutableMerge2.<L, R, D>builder()
						.left(left)
						.right(right)
						.destination(dest)
						.action(action)
						.build();
		}

		static <L> WithLeft<L> given(StateID<L> left) {
				return new WithLeft<L>(left);
		}

		static <L> WithLeft<L> given(Class<L> sourceType) {
				return given(StateID.of(sourceType));
		}

		class WithLeft<L> {
				private final StateID<L> left;
				private WithLeft(StateID<L> left) {
						this.left = left;
				}

				public <R> WithSources<L, R> and(StateID<R> right) {
						return new WithSources<>(left, right);
				}

				public <R> WithSources<L, R> and(Class<R> right) {
						return and(StateID.of(right));
				}
		}

		class WithSources<L, R> {
				private final StateID<L> left;
				private final StateID<R> right;

				public WithSources(StateID<L> left, StateID<R> right) {
						this.left = left;
						this.right = right;
				}

				public <D> WithDestination<L, R, D> state(StateID<D> destination) {
						return new WithDestination<>(left, right, destination);
				}

				public <D> WithDestination<L, R, D> state(Class<D> destination) {
						return state(StateID.of(destination));
				}

		}

		class WithDestination<L, R, D> {
				private final StateID<L> left;
				private final StateID<R> right;
				private final StateID<D> destination;

				public WithDestination(StateID<L> left, StateID<R> right, StateID<D> destination) {
						this.left = left;
						this.right = right;
						this.destination = destination;
				}

				public Merge2<L, R, D> deriveBy(BiFunction<L, R, D> action) {
						return with(action.andThen(State::of));
				}

				public Merge2<L, R, D> with(BiFunction<L, R, State<D>> action) {
						return Merge2.of(left, right, destination, action);
				}

		}

}
