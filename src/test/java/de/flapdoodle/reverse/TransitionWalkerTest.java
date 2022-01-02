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
package de.flapdoodle.reverse;

import de.flapdoodle.reverse.transitions.Derive;
import de.flapdoodle.reverse.transitions.Join;
import de.flapdoodle.reverse.transitions.Start;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;

class TransitionWalkerTest {
	TearDownCounter tearDownCounter;

	@BeforeEach
	public final void before() {
		tearDownCounter = new TearDownCounter();
	}

	private TearDown<String> tearDownListener() {
		return tearDownCounter.listener();
	}

	private void assertTearDowns(String... tearDowns) {
		tearDownCounter.assertTearDownsOrder(tearDowns);
	}

	@Test
	public void startTransitionWorks() {
		List<Transition<?>> transitions = Arrays.asList(
			Start.of(StateID.of(String.class), () -> State.of("hello", tearDownListener()))
		);

		TransitionWalker walker = TransitionWalker.with(transitions);

		try (TransitionWalker.ReachedState<String> state = walker.initState(StateID.of(String.class))) {
			assertEquals("hello", state.current());
		}

		assertTearDowns("hello");
	}

	@Test
	public void startTransitionWithListenerWorks() {
		List<Transition<?>> transitions = Arrays.asList(
			Start.of(StateID.of(String.class), () -> State.of("hello", tearDownListener()))
		);

		TransitionWalker init = TransitionWalker.with(transitions);
		List<String> listenerCalled = new ArrayList<>();

		Listener listener = Listener.builder()
			.onStateReached((type, value) -> {
				assertEquals(StateID.of(String.class), type);
				assertEquals("hello", value);
				listenerCalled.add("up");
			})
			.onTearDown((type, value) -> {
				assertEquals(StateID.of(String.class), type);
				assertEquals("hello", value);
				listenerCalled.add("down");
			})
			.build();

		try (TransitionWalker.ReachedState<String> state = init.initState(StateID.of(String.class), listener)) {
			assertEquals("hello", state.current());
		}

		assertEquals("[up, down]", listenerCalled.toString());
		assertTearDowns("hello");
	}

	@Test
	public void deriveShouldWork() {
		List<Transition<String>> transitions = Arrays.asList(
			Start.of(StateID.of(String.class), () -> State.of("hello", tearDownListener())),
			Derive.of(StateID.of(String.class), StateID.of("bridge", String.class),
				s -> State.of(s + " world", tearDownListener()))
		);

		TransitionWalker init = TransitionWalker.with(transitions);

		try (TransitionWalker.ReachedState<String> state = init.initState(StateID.of("bridge", String.class))) {
			assertEquals("hello world", state.current());
		}

		assertTearDowns("hello world", "hello");
	}

	@Test
	public void transitionMustNotAdressDifferentStatesThanRequired() {
		Transition<String> customTransition=new Transition<String>() {
			@Override public StateID<String> destination() {
				return StateID.of("custom", String.class);
			}
			@Override public Set<StateID<?>> sources() {
				return StateID.setOf(StateID.of(String.class));
			}
			@Override public State<String> result(StateLookup lookup) {
				return State.of("" + lookup.of(StateID.of("foo", String.class)), tearDownListener());
			}
		};

		List<Transition<String>> transitions = Arrays.asList(
			Start.of(StateID.of(String.class), () -> State.of("hello", tearDownListener())),
			customTransition
		);

		TransitionWalker init = TransitionWalker.with(transitions);

		assertThatThrownBy(() -> init.initState(StateID.of("custom", String.class)))
			.isInstanceOf(RuntimeException.class)
			.hasMessage("error on transition to State(custom:String), rollback");
	}

	@Test
	public void joinShouldWork() {
		List<Transition<String>> transitions = Arrays.asList(
			Start.of(StateID.of("hello", String.class), () -> State.of("hello", tearDownListener())),
			Start.of(StateID.of("again", String.class), () -> State.of("again", tearDownListener())),
			Derive.of(StateID.of("hello", String.class), StateID.of("bridge", String.class),
				s -> State.of("[" + s + "]", tearDownListener())),
			Join.of(StateID.of("bridge", String.class), StateID.of("again", String.class),
				StateID.of("merge", String.class),
				(a, b) -> State.of(a + " " + b, tearDownListener()))
		);

		TransitionWalker init = TransitionWalker.with(transitions);

		try (TransitionWalker.ReachedState<String> state = init.initState(StateID.of("merge", String.class))) {
			assertEquals("[hello] again", state.current());
		}

		assertTearDowns("[hello] again", "[hello]", "hello", "again");
	}

	@Test
	public void customTransitionShouldWork() {
		Transition<String> custom=new Transition<String>() {
			private StateID<String> first= StateID.of("depends", String.class);
			private StateID<String> second = StateID.of("again", String.class);

			@Override public StateID<String> destination() {
				return StateID.of("custom", String.class);
			}
			@Override public Set<StateID<?>> sources() {
				return StateID.setOf(first, second);
			}
			@Override public State<String> result(StateLookup lookup) {
				String firstValue = lookup.of(first);
				String secondValue = lookup.of(second);
				return State.of(firstValue+" "+secondValue);
			}
		};

		List<Transition<?>> transitions = Arrays.asList(
			Start.of(StateID.of("hello", String.class), () -> State.of("hello")),
			Start.of(StateID.of("again", String.class), () -> State.of("again")),
			Derive.of(StateID.of("hello", String.class), StateID.of("depends", String.class),
				s -> State.of("[" + s + "]")),

			custom
		);

		TransitionWalker init = TransitionWalker.with(transitions);

		try (TransitionWalker.ReachedState<String> state = init.initState(StateID.of("custom", String.class))) {
			assertEquals("[hello] again", state.current());
		}
	}


	@Test
	public void twoDependencyTransitionWorks() {
		List<Transition<String>> transitions = Arrays.asList(
			Start.of(StateID.of("a", String.class), () -> State.of("hello", tearDownListener())),
			Start.of(StateID.of("b", String.class), () -> State.of("world", tearDownListener())),
			Join.of(StateID.of("a", String.class), StateID.of("b", String.class),
				StateID.of(String.class),
				(a, b) -> State.of(a + " " + b, tearDownListener()))
		);

		TransitionWalker init = TransitionWalker.with(transitions);

		try (TransitionWalker.ReachedState<String> state = init.initState(StateID.of(String.class))) {
			assertEquals("hello world", state.current());
		}

		assertTearDowns("hello world", "hello", "world");
	}

	@Test
	public void multiUsageShouldTearDownAsLast() {
		List<Transition<String>> transitions = Arrays.asList(
			Start.of(StateID.of("a", String.class), () -> State.of("one", tearDownListener())),
			Derive.of(StateID.of("a", String.class), StateID.of("b", String.class),
				a -> State.of("and " + a, tearDownListener())),
			Join.of(StateID.of("a", String.class), StateID.of("b", String.class),
				StateID.of(String.class),
				(a, b) -> State.of(a + " " + b, tearDownListener()))
		);

		TransitionWalker init = TransitionWalker.with(transitions);

		try (TransitionWalker.ReachedState<String> state = init.initState(StateID.of(String.class))) {
			assertEquals("one and one", state.current());
		}

		assertTearDowns("one and one", "and one", "one");
	}

	@Test
	public void tearDownShouldBeCalledOnRollback() {
		List<Transition<String>> transitions = Arrays.asList(
			Start.of(StateID.of(String.class), () -> State.of("hello", tearDownListener())),
			Derive.of(StateID.of(String.class), StateID.of("bridge", String.class), s -> {
				if (true) {
					throw new RuntimeException("--error in transition--");
				}
				return State.of(s + " world", tearDownListener());
			})
		);

		TransitionWalker init = TransitionWalker.with(transitions);

		assertThatThrownBy(() -> init.initState(StateID.of("bridge", String.class)))
			.isInstanceOf(RuntimeException.class)
			.hasMessage("error on transition to State(bridge:String), rollback");

		assertTearDowns("hello");
	}

	@Test
	public void localInitShouldWork() {
		List<Transition<String>> transitions = Arrays.asList(
			Start.of(StateID.of(String.class), () -> State.of("hello", tearDownListener())),
			Derive.of(StateID.of(String.class), StateID.of("bridge", String.class),
				s -> State.of(s + " world", tearDownListener()))
		);

		TransitionWalker init = TransitionWalker.with(transitions);

		try (TransitionWalker.ReachedState<String> state = init.initState(StateID.of(String.class))) {
			assertEquals("hello", state.current());
			try (TransitionWalker.ReachedState<String> subState = state.initState(StateID.of("bridge", String.class))) {
				assertEquals("hello world", subState.current());
			}
			assertTearDowns("hello world");
		}

		assertTearDowns("hello world", "hello");
	}

	@Test
	public void cascadingInitShouldWork() {
		List<Transition<?>> baseRoutes = Arrays.asList(
			Start.of(StateID.of(String.class), () -> State.of("hello", tearDownListener()))
		);

		TransitionWalker baseInit = TransitionWalker.with(baseRoutes);

		List<Transition<?>> transitions = Arrays.asList(
			Start.of(StateID.of(String.class), () -> baseInit.initState(StateID.of(String.class)).asState()),
			Derive.of(StateID.of(String.class), StateID.of("bridge", String.class),
				s -> State.of(s + " world", tearDownListener()))
		);

		TransitionWalker init = TransitionWalker.with(transitions);

		try (TransitionWalker.ReachedState<String> state = init.initState(StateID.of(String.class))) {
			assertEquals("hello", state.current());
			try (TransitionWalker.ReachedState<String> subState = state.initState(StateID.of("bridge", String.class))) {
				assertEquals("hello world", subState.current());
			}
			assertTearDowns("hello world");
		}

		assertTearDowns("hello world", "hello");
	}

	@Test
	public void unknownInitShouldFail() {
		List<Transition<?>> transitions = Arrays.asList(
			Start.of(StateID.of(String.class), () -> State.of("foo"))
		);

		TransitionWalker init = TransitionWalker.with(transitions);

		assertThatThrownBy(() -> init.initState(StateID.of("foo", String.class)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("state State(foo:String) is not part of this init process");

		try (TransitionWalker.ReachedState<String> state = init.initState(StateID.of(String.class))) {
			assertEquals("foo", state.current());
			assertThatThrownBy(() -> state.initState(StateID.of(String.class)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("state State(String) already initialized");
		}
	}

	@Test
	public void missingStartShouldFail() {
		List<Transition<?>> transitions = Arrays.asList(
			Derive.of(StateID.of(String.class), StateID.of("bridge", String.class),
				s -> State.of(s + " world", tearDownListener()))
		);

		TransitionWalker init = TransitionWalker.with(transitions);

		assertThatThrownBy(() -> init.initState(StateID.of("bridge", String.class)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("missing transitions: State(String)");
	}

	@Test
	public void multipleTransitionsToSameDestinationMustFail() {
		List<Transition<?>> transitions = Arrays.asList(
			Start.to(StateID.of(String.class)).initializedWith("first"),
			Start.to(StateID.of("other", String.class)).initializedWith("other"),
			Derive.of(StateID.of(String.class), StateID.of("bridge", String.class),
				s -> State.of(s + " world", tearDownListener())),
			Derive.given(StateID.of("other", String.class)).state(StateID.of("bridge", String.class))
				.with(s -> State.of(s + " world", tearDownListener()))
		);

		assertThatThrownBy(() -> TransitionWalker.with(transitions))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("multiple transitions with same destination:");
	}

	@Test
	public void transitionWalkerAsTransitionWithExposedMissingParts() {
		TransitionWalker walker = Transitions.from(
				Derive.of(StateID.of("inner", String.class), StateID.of("inner-bridge", String.class),
					s -> State.of(s + " world", tearDownListener()))
			).walker();

		Transition<String> transition = walker.asTransitionTo(TransitionMapping
			.builder("wrapped", StateMapping.of(StateID.of("inner-bridge", String.class), StateID.of("bridge", String.class)))
			.addMappings(StateMapping.of(StateID.of(String.class), StateID.of("inner", String.class)))
			.build());

		assertThat(transition.sources())
			.containsExactly(StateID.of(String.class));

		Transitions withWrappedWalker = Transitions.from(
			Start.to(String.class).initializedWith("wrapped"),
			transition
		);

		String dotFile = Transitions.edgeGraphAsDot("wrapped", withWrappedWalker.asGraph());
		System.out.println("--------------------");
		System.out.println(dotFile);
		System.out.println("--------------------");

		try (TransitionWalker.ReachedState<String> state =  withWrappedWalker.walker().initState(StateID.of("bridge", String.class))) {
			assertEquals("wrapped world", state.current());
		}
	}
}