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
package de.flapdoodle.reverse;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Lazy;
import org.immutables.value.Value.Parameter;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * listener MUST NOT throw any exceptions
 */
public interface Listener extends OnStateReached, OnStateTearDown {

	static TypedListener.Builder typedBuilder() {
		return ImmutableTypedListener.builder();
	}

	static ImmutableSimple.Builder builder() {
		return ImmutableSimple.builder();
	}

	static Listener of(BiConsumer<StateID<?>, Object> onStateReached, BiConsumer<StateID<?>, Object> onTearDown) {
		return builder()
			.onStateReached(onStateReached)
			.onTearDown(onTearDown)
			.build();
	}

	@Immutable
	abstract class Simple implements Listener {
		protected abstract Optional<BiConsumer<StateID<?>, Object>> onStateReached();

		protected abstract Optional<BiConsumer<StateID<?>, Object>> onTearDown();

		@Override
		public <T> void onStateReached(StateID<T> state, T value) {
			onStateReached().ifPresent(l -> l.accept(state, value));
		}

		@Override
		public <T> void onStateTearDown(StateID<T> state, T value) {
			onTearDown().ifPresent(l -> l.accept(state, value));
		}
	}

	@Immutable
	abstract class TypedListener implements Listener {

		protected abstract List<StateListener<?>> stateReachedListener();

		protected abstract List<StateListener<?>> stateTearDownListener();

		@Auxiliary
		@Lazy
		protected Map<StateID<?>, Consumer<?>> stateReachedListenerAsMap() {
			return stateReachedListener().stream()
				.collect(Collectors.toMap(StateListener::type, StateListener::listener));
		}

		@Auxiliary
		@Lazy
		protected Map<StateID<?>, Consumer<?>> stateTearDownListenerAsMap() {
			return stateTearDownListener().stream()
				.collect(Collectors.toMap(StateListener::type, StateListener::listener));
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> void onStateReached(StateID<T> state, T value) {
			Optional.ofNullable((Consumer<T>) stateReachedListenerAsMap().get(state))
				.ifPresent(c -> c.accept(value));
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> void onStateTearDown(StateID<T> state, T value) {
			Optional.ofNullable((Consumer<T>) stateTearDownListenerAsMap().get(state))
				.ifPresent(c -> c.accept(value));
		}
		public interface Builder {
			Builder addStateReachedListener(StateListener<?> listener);

			Builder addStateTearDownListener(StateListener<?> listener);

			default <T> Builder onStateReached(StateID<T> type, Consumer<T> listener) {
				return addStateReachedListener(StateListener.of(type, listener));
			}

			default <T> Builder onStateTearDown(StateID<T> type, Consumer<T> listener) {
				return addStateTearDownListener(StateListener.of(type, listener));
			}

			Listener build();
		}

	}

	@Immutable
	interface StateListener<T> {
		@Parameter
		StateID<T> type();

		@Parameter
		Consumer<T> listener();

		static <T> StateListener<T> of(StateID<T> type, Consumer<T> listener) {
			return ImmutableStateListener.of(type, listener);
		}
	}

}
