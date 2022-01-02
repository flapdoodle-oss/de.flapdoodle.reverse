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
package de.flapdoodle.reverse.transitions;

import de.flapdoodle.reverse.State;
import de.flapdoodle.reverse.StateID;
import de.flapdoodle.reverse.StateLookup;
import de.flapdoodle.reverse.Transition;
import de.flapdoodle.reverse.naming.HasLabel;
import org.immutables.value.Value;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;

@Value.Immutable
public abstract class Join<L, R, D> implements Transition<D>, HasLabel {
	public abstract StateID<L> left();

	public abstract StateID<R> right();

	public abstract StateID<D> destination();

	protected abstract BiFunction<L, R, State<D>> action();

	@Value.Default
	@Override
	public String transitionLabel() {
		return "Join";
	}

	@Override
	@Value.Lazy
	public Set<StateID<?>> sources() {
		return new HashSet<>(Arrays.asList(left(), right()));
	}

	@Override
	@Value.Auxiliary
	public State<D> result(StateLookup lookup) {
		return action().apply(lookup.of(left()), lookup.of(right()));
	}

	public static <L, R, D> ImmutableJoin<L, R, D> of(StateID<L> left, StateID<R> right, StateID<D> dest, BiFunction<L, R, State<D>> action) {
		return ImmutableJoin.<L, R, D>builder()
			.left(left)
			.right(right)
			.destination(dest)
			.action(action)
			.build();
	}

	public static <L> WithLeft<L> given(StateID<L> left) {
		return new WithLeft<L>(left);
	}

	public static <L> WithLeft<L> given(Class<L> sourceType) {
		return given(StateID.of(sourceType));
	}

	public static class WithLeft<L> {
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

	public static class WithSources<L, R> {
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

	public static class WithDestination<L, R, D> {
		private final StateID<L> left;
		private final StateID<R> right;
		private final StateID<D> destination;

		public WithDestination(StateID<L> left, StateID<R> right, StateID<D> destination) {
			this.left = left;
			this.right = right;
			this.destination = destination;
		}

		public ImmutableJoin<L, R, D> deriveBy(BiFunction<L, R, D> action) {
			return with(action.andThen(State::of));
		}

		public ImmutableJoin<L, R, D> with(BiFunction<L, R, State<D>> action) {
			return Join.of(left, right, destination, action);
		}

	}

}
