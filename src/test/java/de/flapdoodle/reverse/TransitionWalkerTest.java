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

import de.flapdoodle.reverse.graph.TransitionGraph;
import de.flapdoodle.reverse.transitions.Derive;
import de.flapdoodle.reverse.transitions.Join;
import de.flapdoodle.reverse.transitions.Start;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
		Transitions transitions = Transitions.from(
			Start.of(StateID.of(String.class), () -> State.of("hello", tearDownListener()))
		);

		TransitionWalker walker = transitions.walker();

		try (TransitionWalker.ReachedState<String> state = walker.initState(StateID.of(String.class))) {
			assertThat(state.current()).isEqualTo("hello");
		}

		assertTearDowns("hello");
	}

	@Test
	public void startTransitionWithListenerWorks() {
		Transitions transitions = Transitions.from(
			Start.of(StateID.of(String.class), () -> State.of("hello", tearDownListener()))
		);

		TransitionWalker init = transitions.walker();
		List<String> listenerCalled = new ArrayList<>();

		Listener listener = Listener.builder()
			.onStateReached((type, value) -> {
				assertThat(type).isEqualTo(StateID.of(String.class));
				assertThat((String) value).isEqualTo("hello");
				listenerCalled.add("up");
			})
			.onTearDown((type, value) -> {
				assertThat(type).isEqualTo(StateID.of(String.class));
				assertThat((String) value).isEqualTo("hello");
				listenerCalled.add("down");
			})
			.build();

		try (TransitionWalker.ReachedState<String> state = init.initState(StateID.of(String.class), listener)) {
			assertThat(state.current()).isEqualTo("hello");
		}

		assertThat(listenerCalled.toString()).isEqualTo("[up, down]");
		assertTearDowns("hello");
	}

	@Test
	public void deriveShouldWork() {
		Transitions transitions = Transitions.from(
			Start.of(StateID.of(String.class), () -> State.of("hello", tearDownListener())),
			Derive.of(StateID.of(String.class), StateID.of("bridge", String.class),
				s -> State.of(s + " world", tearDownListener()))
		);

		TransitionWalker init = transitions.walker();

		try (TransitionWalker.ReachedState<String> state = init.initState(StateID.of("bridge", String.class))) {
			assertThat(state.current()).isEqualTo("hello world");
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

		Transitions transitions = Transitions.from(
			Start.of(StateID.of(String.class), () -> State.of("hello", tearDownListener())),
			customTransition
		);

		TransitionWalker init = transitions.walker();

		assertThatThrownBy(() -> init.initState(StateID.of("custom", String.class)))
			.isInstanceOf(RuntimeException.class)
			.hasMessage("rollback after error on transition to State(custom:String), successful reached:\n"
				+ "  State(String)=hello\n");
	}

	@Test
	public void joinShouldWork() {
		Transitions transitions = Transitions.from(
			Start.of(StateID.of("hello", String.class), () -> State.of("hello", tearDownListener())),
			Start.of(StateID.of("again", String.class), () -> State.of("again", tearDownListener())),
			Derive.of(StateID.of("hello", String.class), StateID.of("bridge", String.class),
				s -> State.of("[" + s + "]", tearDownListener())),
			Join.of(StateID.of("bridge", String.class), StateID.of("again", String.class),
				StateID.of("merge", String.class),
				(a, b) -> State.of(a + " " + b, tearDownListener()))
		);

		TransitionWalker init = transitions.walker();

		try (TransitionWalker.ReachedState<String> state = init.initState(StateID.of("merge", String.class))) {
			assertThat(state.current()).isEqualTo("[hello] again");
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

		Transitions transitions = Transitions.from(
			Start.of(StateID.of("hello", String.class), () -> State.of("hello")),
			Start.of(StateID.of("again", String.class), () -> State.of("again")),
			Derive.of(StateID.of("hello", String.class), StateID.of("depends", String.class),
				s -> State.of("[" + s + "]")),

			custom
		);

		TransitionWalker init = transitions.walker();

		try (TransitionWalker.ReachedState<String> state = init.initState(StateID.of("custom", String.class))) {
			assertThat(state.current()).isEqualTo("[hello] again");
		}
	}

	@Test
	public void twoDependencyTransitionWorks() {
		Transitions transitions = Transitions.from(
			Start.of(StateID.of("a", String.class), () -> State.of("hello", tearDownListener())),
			Start.of(StateID.of("b", String.class), () -> State.of("world", tearDownListener())),
			Join.of(StateID.of("a", String.class), StateID.of("b", String.class),
				StateID.of(String.class),
				(a, b) -> State.of(a + " " + b, tearDownListener()))
		);

		TransitionWalker init = transitions.walker();

		try (TransitionWalker.ReachedState<String> state = init.initState(StateID.of(String.class))) {
			assertThat(state.current()).isEqualTo("hello world");
		}

		assertTearDowns("hello world", "hello", "world");
	}

	@Test
	public void multiUsageShouldTearDownAsLast() {
		Transitions transitions = Transitions.from(
			Start.of(StateID.of("a", String.class), () -> State.of("one", tearDownListener())),
			Derive.of(StateID.of("a", String.class), StateID.of("b", String.class),
				a -> State.of("and " + a, tearDownListener())),
			Join.of(StateID.of("a", String.class), StateID.of("b", String.class),
				StateID.of(String.class),
				(a, b) -> State.of(a + " " + b, tearDownListener()))
		);

		TransitionWalker init = transitions.walker();

		try (TransitionWalker.ReachedState<String> state = init.initState(StateID.of(String.class))) {
			assertThat(state.current()).isEqualTo("one and one");
		}

		assertTearDowns("one and one", "and one", "one");
	}

	@Test
	public void tearDownShouldBeCalledOnRollback() {
		Transitions transitions = Transitions.from(
			Start.of(StateID.of(String.class), () -> State.of("hello", tearDownListener())),
			Derive.of(StateID.of(String.class), StateID.of("bridge", String.class), s -> {
				if (true) {
					throw new RuntimeException("--error in transition--");
				}
				return State.of(s + " world", tearDownListener());
			})
		);

		TransitionWalker init = transitions.walker();

		assertThatThrownBy(() -> init.initState(StateID.of("bridge", String.class)))
			.isInstanceOf(RuntimeException.class)
			.hasMessage("rollback after error on transition to State(bridge:String), successful reached:\n"
				+ "  State(String)=hello\n");

		assertTearDowns("hello");
	}

	@Test
	public void localInitShouldWork() {
		Transitions transitions = Transitions.from(
			Start.of(StateID.of(String.class), () -> State.of("hello", tearDownListener())),
			Derive.of(StateID.of(String.class), StateID.of("bridge", String.class),
				s -> State.of(s + " world", tearDownListener()))
		);

		TransitionWalker init = transitions.walker();

		try (TransitionWalker.ReachedState<String> state = init.initState(StateID.of(String.class))) {
			assertThat(state.current()).isEqualTo("hello");
			try (TransitionWalker.ReachedState<String> subState = state.initState(StateID.of("bridge", String.class))) {
				assertThat(subState.current()).isEqualTo("hello world");
			}
			assertTearDowns("hello world");
		}

		assertTearDowns("hello world", "hello");
	}

	@Test
	public void cascadingInitShouldWork() {
		Transitions baseRoutes = Transitions.from(
			Start.of(StateID.of(String.class), () -> State.of("hello", tearDownListener()))
		);

		TransitionWalker baseInit = baseRoutes.walker();

		Transitions transitions = Transitions.from(
			Start.of(StateID.of(String.class), () -> baseInit.initState(StateID.of(String.class)).asState()),
			Derive.of(StateID.of(String.class), StateID.of("bridge", String.class),
				s -> State.of(s + " world", tearDownListener()))
		);

		TransitionWalker init = transitions.walker();

		try (TransitionWalker.ReachedState<String> state = init.initState(StateID.of(String.class))) {
			assertThat(state.current()).isEqualTo("hello");
			try (TransitionWalker.ReachedState<String> subState = state.initState(StateID.of("bridge", String.class))) {
				assertThat(subState.current()).isEqualTo("hello world");
			}
			assertTearDowns("hello world");
		}

		assertTearDowns("hello world", "hello");
	}

	@Test
	public void unknownInitShouldFail() {
		Transitions transitions = Transitions.from(
			Start.of(StateID.of(String.class), () -> State.of("foo"))
		);

		TransitionWalker init = transitions.walker();

		assertThatThrownBy(() -> init.initState(StateID.of("foo", String.class)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("state State(foo:String) is not part of this init process");

		try (TransitionWalker.ReachedState<String> state = init.initState(StateID.of(String.class))) {
			assertThat(state.current()).isEqualTo("foo");
			assertThatThrownBy(() -> state.initState(StateID.of(String.class)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("state State(String) already initialized");
		}
	}

	@Test
	public void missingStartShouldFail() {
		Transitions transitions = Transitions.from(
			Derive.of(StateID.of(String.class), StateID.of("bridge", String.class),
				s -> State.of(s + " world", tearDownListener()))
		);

		TransitionWalker init = transitions.walker();

		assertThatThrownBy(() -> init.initState(StateID.of("bridge", String.class)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("missing transitions: State(String)");
	}

	@Test
	public void multipleTransitionsToSameDestinationMustFail() {
		List<? extends Transition<String>> transitions = Arrays.asList(
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

		String dotFile = TransitionGraph.edgeGraphAsDot("wrapped", withWrappedWalker);
		System.out.println("--------------------");
		System.out.println(dotFile);
		System.out.println("--------------------");

		try (TransitionWalker.ReachedState<String> state =  withWrappedWalker.walker().initState(StateID.of("bridge", String.class))) {
			assertThat(state.current()).isEqualTo("wrapped world");
		}
	}

	@Test
	public void multipleExceptionsInTearDownMustShowInStackTrace() {
		StateID<String> start = StateID.of("start", String.class);
		StateID<String> middle = StateID.of("middle", String.class);
		StateID<String> end = StateID.of("end", String.class);

		assertThatThrownBy(() -> {
			try (TransitionWalker.ReachedState<String> endReached = Transitions.from(
					Start.to(start)
						.with(() -> State.of("START", throwRuntimeExceptionOnTearDown("teardown start"))),
					Derive.given(start).state(middle)
						.with(s -> State.of("MIDDLE", throwRuntimeExceptionOnTearDown("teardown middle"))),
					Derive.given(middle).state(end)
						.with(s -> State.of("END", throwRuntimeExceptionOnTearDown("teardown end")))
				).walker()
				.initState(end)) {

			}
		})
			.isInstanceOf(TearDownException.class)
			.asInstanceOf(InstanceOfAssertFactories.type(TearDownException.class))
			.extracting(Throwable::getSuppressed)
			.asInstanceOf(InstanceOfAssertFactories.array(Throwable[].class))
			.hasSize(3)
			.satisfies(it -> {
				assertThat(it)
					.anySatisfy(ex -> assertThat(ex).hasMessageContaining("teardown start"))
					.anySatisfy(ex -> assertThat(ex).hasMessageContaining("teardown middle"))
					.anySatisfy(ex -> assertThat(ex).hasMessageContaining("teardown end"));
			});
	}

	private static <T> TearDown<T> throwRuntimeExceptionOnTearDown(String message) {
		return ignore -> {
			throw new RuntimeException(message);
		};
	}
}