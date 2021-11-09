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
import de.flapdoodle.transition.types.TriFunction;
import org.immutables.value.Value;

@Value.Immutable
public interface Merge3<L, M, R, D> extends Edge<D> {
		StateID<L> left();
		StateID<M> middle();
		StateID<R> right();
		StateID<D> destination();
		TriFunction<L, M, R, State<D>> action();

		static <L, M, R, D> Merge3<L, M, R, D> of(StateID<L> left, StateID<M> middle, StateID<R> right, StateID<D> dest, TriFunction<L, M, R, State<D>> action) {
				return ImmutableMerge3.<L, M, R ,D>builder()
						.left(left)
						.middle(middle)
						.right(right)
						.destination(dest)
						.action(action)
						.build();
		}

		static <L> WithLeft<L> given(StateID<L> left) {
				return new WithLeft<L>(left);
		}

		static <L> WithLeft<L> given(Class<L> leftType) {
				return given(StateID.of(leftType));
		}

		class WithLeft<L> {
				private final StateID<L> left;
				private WithLeft(StateID<L> left) {
						this.left = left;
				}

				public <M> WithMiddle<L, M> and(StateID<M> middle) {
						return new WithMiddle<>(left, middle);
				}

				public <M> WithMiddle<L, M> and(Class<M> middle) {
						return and(StateID.of(middle));
				}
		}

		class WithMiddle<L, M> {
				private final StateID<L> left;
				private final StateID<M> middle;

				public WithMiddle(StateID<L> left, StateID<M> middle) {
						this.left = left;
						this.middle = middle;
				}

				public <R> WithSources<L, M, R> and(StateID<R> right) {
						return new WithSources<L, M, R>(left, middle, right);
				}

				public <R> WithSources<L, M, R> and(Class<R> destination) {
						return and(StateID.of(destination));
				}

		}

		class WithSources<L, M, R> {
				private final StateID<L> left;
				private final StateID<M> middle;
				private final StateID<R> right;

				public WithSources(StateID<L> left, StateID<M> middle, StateID<R> right) {
						this.left = left;
						this.middle = middle;
						this.right = right;
				}

				public <D> WithDestination<L, M, R, D> state(StateID<D> destination) {
						return new WithDestination<>(left, middle, right, destination);
				}

				public <D> WithDestination<L, M, R, D> state(Class<D> destination) {
						return state(StateID.of(destination));
				}

		}

		class WithDestination<L, M, R, D> {
				private final StateID<L> left;
				private final StateID<M> middle;
				private final StateID<R> right;
				private final StateID<D> destination;

				public WithDestination(StateID<L> left, StateID<M> middle, StateID<R> right, StateID<D> destination) {
						this.left = left;
						this.middle = middle;
						this.right = right;
						this.destination = destination;
				}

				public Merge3<L, M, R, D> deriveBy(TriFunction<L, M, R, D> action) {
						return with(action.andThen(State::of));
				}

				public Merge3<L, M, R, D> with(TriFunction<L, M, R, State<D>> action) {
						return Merge3.of(left, middle, right, destination, action);
				}

		}


}
