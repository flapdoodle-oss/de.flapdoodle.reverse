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
package de.flapdoodle.reverse.transitions;

import de.flapdoodle.reverse.State;
import de.flapdoodle.reverse.StateID;
import de.flapdoodle.reverse.StateLookup;
import de.flapdoodle.reverse.Transition;
import de.flapdoodle.reverse.naming.HasLabel;
import de.flapdoodle.reverse.types.TriFunction;
import org.immutables.value.Value;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

@Value.Immutable
public abstract class Combine<A, B, C, D> implements Transition<D>, HasLabel {
	public abstract StateID<A> first();

	public abstract StateID<B> second();
	
	public abstract StateID<C> third();

	public abstract StateID<D> destination();

	protected abstract TriFunction<A, B, C, State<D>> action();

	@Value.Default
	@Override
	public String transitionLabel() {
		return "Combine";
	}

	@Override
	@Value.Lazy
	public Set<StateID<?>> sources() {
		return new HashSet<>(Arrays.asList(first(), second(), third()));
	}

	@Override
	@Value.Auxiliary
	public State<D> result(StateLookup lookup) {
		return action().apply(lookup.of(first()), lookup.of(second()), lookup.of(third()));
	}

	public static <A, B, C, D> ImmutableCombine<A, B, C, D> of(StateID<A> first, StateID<B> second, StateID<C> third, StateID<D> dest, TriFunction<A, B, C, State<D>> action) {
		return ImmutableCombine.<A, B, C, D>builder()
			.first(first)
			.second(second)
			.third(third)
			.destination(dest)
			.action(action)
			.build();
	}

	public static <A> WithFirst<A> given(StateID<A> first) {
		return new WithFirst<A>(first);
	}

	public static <A> WithFirst<A> given(Class<A> sourceType) {
		return given(StateID.of(sourceType));
	}

	public static class WithFirst<A> {
		private final StateID<A> first;
		private WithFirst(StateID<A> first) {
			this.first = first;
		}

		public <B> WithFirstAndSecond<A, B> and(StateID<B> second) {
			return new WithFirstAndSecond<>(first, second);
		}

		public <B> WithFirstAndSecond<A, B> and(Class<B> second) {
			return and(StateID.of(second));
		}
	}

	public static class WithFirstAndSecond<A, B> {
		private final StateID<A> first;
		private final StateID<B> second;

		public WithFirstAndSecond(StateID<A> first, StateID<B> second) {
			this.first = first;
			this.second = second;
		}

		public <C> WithThreeSources<A, B, C> and(StateID<C> third) {
			return new WithThreeSources<>(first, second, third);
		}

		public <C> WithThreeSources<A, B, C> and(Class<C> third) {
			return and(StateID.of(third));
		}
	}

	public static class WithThreeSources<A, B, C> {
		private final StateID<A> first;
		private final StateID<B> second;
		private final StateID<C> third;

		public WithThreeSources(StateID<A> first, StateID<B> second, StateID<C> third) {
			this.first = first;
			this.second = second;
			this.third = third;
		}

		public <D> WithDestination<A, B, C, D> state(StateID<D> destination) {
			return new WithDestination<>(first, second, third, destination);
		}

		public <D> WithDestination<A, B, C, D> state(Class<D> destination) {
			return state(StateID.of(destination));
		}
	}

	public static class WithDestination<A, B, C, D> {
		private final StateID<A> first;
		private final StateID<B> second;
		private final StateID<C> third;
		private final StateID<D> destination;

		public WithDestination(StateID<A> first, StateID<B> second, StateID<C> third, StateID<D> destination) {
			this.first = first;
			this.second = second;
			this.third = third;
			this.destination = destination;
		}

		public ImmutableCombine<A, B, C, D> deriveBy(TriFunction<A, B, C, D> action) {
			return with(action.andThen(State::of));
		}

		public ImmutableCombine<A, B, C, D> with(TriFunction<A, B, C, State<D>> action) {
			return Combine.of(first, second, third, destination, action);
		}
	}
}