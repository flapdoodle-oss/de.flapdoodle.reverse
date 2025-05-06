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
import de.flapdoodle.reverse.types.QuadFunction;
import org.immutables.value.Value;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A transition that merges four source states to create a destination state.
 * <p>
 * This transition requires four source states of types A, B, C, and D to produce
 * a destination state of type R. It uses a {@link QuadFunction} to combine the
 * source values and produce the destination state.
 * 
 * <p>Example usage:
 * <pre>
 * {@code
 * Transition<String> merged = Merge.given(firstState)
 *     .and(secondState)
 *     .and(thirdState)
 *     .and(fourthState)
 *     .state(resultState)
 *     .deriveBy((a, b, c, d) -> a + b + c + d);
 * }
 * </pre>
 *
 * @param <A> the type of the first source state
 * @param <B> the type of the second source state
 * @param <C> the type of the third source state
 * @param <D> the type of the fourth source state
 * @param <R> the type of the destination state
 */
@Value.Immutable
public abstract class Merge<A, B, C, D, R> implements Transition<R>, HasLabel {
	
	/**
	 * Returns the StateID for the first source state.
	 *
	 * @return the StateID of the first source state
	 */
	public abstract StateID<A> first();

	/**
	 * Returns the StateID for the second source state.
	 *
	 * @return the StateID of the second source state
	 */
	public abstract StateID<B> second();
	
	/**
	 * Returns the StateID for the third source state.
	 *
	 * @return the StateID of the third source state
	 */
	public abstract StateID<C> third();
	
	/**
	 * Returns the StateID for the fourth source state.
	 *
	 * @return the StateID of the fourth source state
	 */
	public abstract StateID<D> fourth();

	/**
	 * Returns the StateID for the destination state.
	 *
	 * @return the StateID of the destination state
	 */
	public abstract StateID<R> destination();

	/**
	 * Returns the action function that transforms the four source states into the destination state.
	 *
	 * @return the action function
	 */
	protected abstract QuadFunction<A, B, C, D, State<R>> action();

	/**
	 * {@inheritDoc}
	 */
	@Value.Default
	@Override
	public String transitionLabel() {
		return "Merge";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Value.Lazy
	public Set<StateID<?>> sources() {
		return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(first(), second(), third(), fourth())));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Value.Auxiliary
	public State<R> result(StateLookup lookup) {
		Objects.requireNonNull(lookup, "lookup must not be null");
		return action().apply(lookup.of(first()), lookup.of(second()), lookup.of(third()), lookup.of(fourth()));
	}

	/**
	 * Creates a new Merge transition.
	 *
	 * @param <A> the type of the first source state
	 * @param <B> the type of the second source state
	 * @param <C> the type of the third source state
	 * @param <D> the type of the fourth source state
	 * @param <R> the type of the destination state
	 * @param first the StateID of the first source state
	 * @param second the StateID of the second source state
	 * @param third the StateID of the third source state
	 * @param fourth the StateID of the fourth source state
	 * @param dest the StateID of the destination state
	 * @param action the function to merge source states into destination state
	 * @return a new Merge transition
	 */
	public static <A, B, C, D, R> ImmutableMerge<A, B, C, D, R> of(
		StateID<A> first, StateID<B> second, StateID<C> third, StateID<D> fourth, 
		StateID<R> dest, QuadFunction<A, B, C, D, State<R>> action) {
		return ImmutableMerge.<A, B, C, D, R>builder()
			.first(first)
			.second(second)
			.third(third)
			.fourth(fourth)
			.destination(dest)
			.action(action)
			.build();
	}

	/**
	 * Starts building a Merge transition with the given first source state.
	 *
	 * @param <A> the type of the first source state
	 * @param first the StateID of the first source state
	 * @return a builder to continue creating the transition
	 */
	public static <A> WithFirst<A> given(StateID<A> first) {
		Objects.requireNonNull(first, "first must not be null");
		return new WithFirst<A>(first);
	}

	/**
	 * Starts building a Merge transition with a StateID created from the given class.
	 *
	 * @param <A> the type of the first source state
	 * @param sourceType the class to create a StateID from
	 * @return a builder to continue creating the transition
	 */
	public static <A> WithFirst<A> given(Class<A> sourceType) {
		Objects.requireNonNull(sourceType, "sourceType must not be null");
		return given(StateID.of(sourceType));
	}

	/**
	 * Builder class for creating a Merge transition, step 1: add second source.
	 *
	 * @param <A> the type of the first source state
	 */
	public static class WithFirst<A> {
		private final StateID<A> first;
		private WithFirst(StateID<A> first) {
			this.first = first;
		}

		/**
		 * Adds the second source state to the transition.
		 *
		 * @param <B> the type of the second source state
		 * @param second the StateID of the second source state
		 * @return a builder to continue creating the transition
		 */
		public <B> WithFirstAndSecond<A, B> and(StateID<B> second) {
			Objects.requireNonNull(second, "second must not be null");
			return new WithFirstAndSecond<>(first, second);
		}

		/**
		 * Adds the second source state to the transition with a StateID created from the given class.
		 *
		 * @param <B> the type of the second source state
		 * @param second the class to create a StateID from
		 * @return a builder to continue creating the transition
		 */
		public <B> WithFirstAndSecond<A, B> and(Class<B> second) {
			Objects.requireNonNull(second, "second must not be null");
			return and(StateID.of(second));
		}
	}

	/**
	 * Builder class for creating a Merge transition, step 2: add third source.
	 *
	 * @param <A> the type of the first source state
	 * @param <B> the type of the second source state
	 */
	public static class WithFirstAndSecond<A, B> {
		private final StateID<A> first;
		private final StateID<B> second;

		public WithFirstAndSecond(StateID<A> first, StateID<B> second) {
			this.first = first;
			this.second = second;
		}

		/**
		 * Adds the third source state to the transition.
		 *
		 * @param <C> the type of the third source state
		 * @param third the StateID of the third source state
		 * @return a builder to continue creating the transition
		 */
		public <C> WithThreeSources<A, B, C> and(StateID<C> third) {
			Objects.requireNonNull(third, "third must not be null");
			return new WithThreeSources<>(first, second, third);
		}

		/**
		 * Adds the third source state to the transition with a StateID created from the given class.
		 *
		 * @param <C> the type of the third source state
		 * @param third the class to create a StateID from
		 * @return a builder to continue creating the transition
		 */
		public <C> WithThreeSources<A, B, C> and(Class<C> third) {
			Objects.requireNonNull(third, "third must not be null");
			return and(StateID.of(third));
		}
	}

	/**
	 * Builder class for creating a Merge transition, step 3: add fourth source.
	 *
	 * @param <A> the type of the first source state
	 * @param <B> the type of the second source state
	 * @param <C> the type of the third source state
	 */
	public static class WithThreeSources<A, B, C> {
		private final StateID<A> first;
		private final StateID<B> second;
		private final StateID<C> third;

		public WithThreeSources(StateID<A> first, StateID<B> second, StateID<C> third) {
			this.first = first;
			this.second = second;
			this.third = third;
		}

		/**
		 * Adds the fourth source state to the transition.
		 *
		 * @param <D> the type of the fourth source state
		 * @param fourth the StateID of the fourth source state
		 * @return a builder to continue creating the transition
		 */
		public <D> WithFourSources<A, B, C, D> and(StateID<D> fourth) {
			Objects.requireNonNull(fourth, "fourth must not be null");
			return new WithFourSources<>(first, second, third, fourth);
		}

		/**
		 * Adds the fourth source state to the transition with a StateID created from the given class.
		 *
		 * @param <D> the type of the fourth source state
		 * @param fourth the class to create a StateID from
		 * @return a builder to continue creating the transition
		 */
		public <D> WithFourSources<A, B, C, D> and(Class<D> fourth) {
			Objects.requireNonNull(fourth, "fourth must not be null");
			return and(StateID.of(fourth));
		}
	}
	
	/**
	 * Builder class for creating a Merge transition, step 4: specify destination.
	 *
	 * @param <A> the type of the first source state
	 * @param <B> the type of the second source state
	 * @param <C> the type of the third source state
	 * @param <D> the type of the fourth source state
	 */
	public static class WithFourSources<A, B, C, D> {
		private final StateID<A> first;
		private final StateID<B> second;
		private final StateID<C> third;
		private final StateID<D> fourth;

		public WithFourSources(StateID<A> first, StateID<B> second, StateID<C> third, StateID<D> fourth) {
			this.first = first;
			this.second = second;
			this.third = third;
			this.fourth = fourth;
		}

		/**
		 * Sets the destination state for the transition.
		 *
		 * @param <R> the type of the destination state
		 * @param destination the StateID of the destination state
		 * @return a builder to finalize the transition
		 */
		public <R> WithDestination<A, B, C, D, R> state(StateID<R> destination) {
			Objects.requireNonNull(destination, "destination must not be null");
			return new WithDestination<>(first, second, third, fourth, destination);
		}

		/**
		 * Sets the destination state for the transition with a StateID created from the given class.
		 *
		 * @param <R> the type of the destination state
		 * @param destination the class to create a StateID from
		 * @return a builder to finalize the transition
		 */
		public <R> WithDestination<A, B, C, D, R> state(Class<R> destination) {
			Objects.requireNonNull(destination, "destination must not be null");
			return state(StateID.of(destination));
		}
	}

	/**
	 * Final builder class for creating a Merge transition.
	 *
	 * @param <A> the type of the first source state
	 * @param <B> the type of the second source state
	 * @param <C> the type of the third source state
	 * @param <D> the type of the fourth source state
	 * @param <R> the type of the destination state
	 */
	public static class WithDestination<A, B, C, D, R> {
		private final StateID<A> first;
		private final StateID<B> second;
		private final StateID<C> third;
		private final StateID<D> fourth;
		private final StateID<R> destination;

		public WithDestination(StateID<A> first, StateID<B> second, StateID<C> third, StateID<D> fourth, StateID<R> destination) {
			this.first = first;
			this.second = second;
			this.third = third;
			this.fourth = fourth;
			this.destination = destination;
		}

		/**
		 * Provides a function to derive the destination value from the four source values.
		 * The function will automatically be wrapped to create a State with the result.
		 *
		 * @param action a function that takes four source values and returns a destination value
		 * @return a complete Merge transition
		 */
		public ImmutableMerge<A, B, C, D, R> deriveBy(QuadFunction<A, B, C, D, R> action) {
			Objects.requireNonNull(action, "action must not be null");
			return with(action.andThen(State::of));
		}

		/**
		 * Provides a function to derive the destination state from the four source values.
		 * This allows for custom state creation with teardown handling.
		 *
		 * @param action a function that takes four source values and returns a destination state
		 * @return a complete Merge transition
		 */
		public ImmutableMerge<A, B, C, D, R> with(QuadFunction<A, B, C, D, State<R>> action) {
			Objects.requireNonNull(action, "action must not be null");
			return Merge.of(first, second, third, fourth, destination, action);
		}
	}
}