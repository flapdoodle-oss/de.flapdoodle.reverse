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

import de.flapdoodle.reverse.transitions.Derive;
import de.flapdoodle.reverse.transitions.Join;
import de.flapdoodle.reverse.transitions.Start;
import de.flapdoodle.testdoc.Recorder;
import de.flapdoodle.testdoc.Recording;
import de.flapdoodle.testdoc.TabSize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HowToBuildAndUseTransitionsTest {
	TearDownCounter tearDownCounter;

	@RegisterExtension
	public static Recording recording = Recorder.with("HowToBuildAndUseTransitions.md", TabSize.spaces(2));

	@BeforeEach
	public final void before() {
		tearDownCounter = new TearDownCounter();
	}

	private <T> TearDown<T> tearDownListener() {
		return tearDownCounter.listener();
	}

	@Test
	public void vertex() {
		recording.begin();
		StateID<String> id = StateID.of(String.class);
		StateID<String> idWithLabel = StateID.of("foo", String.class);
		recording.end();
	}

	@Test
	public void transitions() {
		recording.begin();
		Start<String> start;
		Derive<String, String> derive;
		Join<String, String, String> merge;

		start = Start.of(StateID.of(String.class), () -> State.of(""));
		derive = Derive.of(StateID.of("a", String.class), StateID.of("b", String.class), State::of);
		merge = Join.of(StateID.of("left", String.class), StateID.of("right", String.class),
			StateID.of("merged", String.class), (a, b) -> State.of(a + b));
		recording.end();
	}

	@Test
	public void fluentTransitions() {
		recording.begin();
		Start<String> start;
		Derive<String, String> derive;
		Join<String, String, String> merge;

		start = Start.to(String.class).initializedWith("");
		derive = Derive.given(StateID.of("a", String.class)).state(StateID.of("b", String.class)).deriveBy(it -> it);
		merge = Join.given(StateID.of("left", String.class)).and(StateID.of("right", String.class))
			.state(StateID.of("merged", String.class)).deriveBy((a, b) -> a + b);
		recording.end();
	}
	@Test
	public void state() {
		recording.begin();
		State<String> state = State.builder("hello")
			.onTearDown(value -> System.out.println("tearDown " + value))
			.build();
		recording.end();
	}

	@Test
	public void startTransitionWorks() {
		recording.begin();
		Transitions transitions = Transitions.from(
			Start.of(StateID.of(String.class), () -> State.of("hello"))
		);

		TransitionWalker walker = transitions.walker();

		try (TransitionWalker.ReachedState<String> state = walker.initState(StateID.of(String.class))) {
			assertEquals("hello", state.current());
		}

		recording.end();
	}

	@Test
	public void deriveShouldWork() {
		recording.begin();
		Transitions transitions = Transitions.from(
			Start.of(StateID.of(String.class), () -> State.of("hello")),
			Derive.of(StateID.of(String.class), StateID.of("depends", String.class), s -> State.of(s + " world"))
		);

		TransitionWalker walker = transitions.walker();

		try (TransitionWalker.ReachedState<String> state = walker.initState(StateID.of("depends", String.class))) {
			assertEquals("hello world", state.current());
		}
		recording.end();
	}

	@Test
	public void joinShouldWork() {
		recording.begin();
		Transitions transitions = Transitions.from(
			Start.of(StateID.of("hello", String.class), () -> State.of("hello")),
			Start.of(StateID.of("again", String.class), () -> State.of("again")),
			Derive.of(StateID.of("hello", String.class), StateID.of("depends", String.class),
				s -> State.of("[" + s + "]")),

			Join.of(StateID.of("depends", String.class), StateID.of("again", String.class),
				StateID.of("merge", String.class),
				(a, b) -> State.of(a + " " + b))
		);

		TransitionWalker walker = transitions.walker();

		try (TransitionWalker.ReachedState<String> state = walker.initState(StateID.of("merge", String.class))) {
			assertEquals("[hello] again", state.current());
		}
		recording.end();
	}

	@Test
	public void customTransitionShouldWork() {
		recording.begin();
		Transition<String> custom = new Transition<String>() {
			private StateID<String> first = StateID.of("depends", String.class);
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
				return State.of(firstValue + " " + secondValue);
			}
		};

		Transitions transitions = Transitions.from(
			Start.of(StateID.of("hello", String.class), () -> State.of("hello")),
			Start.of(StateID.of("again", String.class), () -> State.of("again")),
			Derive.of(StateID.of("hello", String.class), StateID.of("depends", String.class),
				s -> State.of("[" + s + "]")),

			custom
		);

		TransitionWalker walker = transitions.walker();

		try (TransitionWalker.ReachedState<String> state = walker.initState(StateID.of("custom", String.class))) {
			assertEquals("[hello] again", state.current());
		}
		recording.end();
	}

	@Test
	public void localInitShouldWork() {
		recording.begin();
		Transitions transitions = Transitions.from(
			Start.of(StateID.of(String.class), () -> State.of("hello", tearDownListener())),
			Derive.of(StateID.of(String.class), StateID.of("depends", String.class), s -> State.of(s + " world", tearDownListener()))
		);

		TransitionWalker walker = transitions.walker();

		try (TransitionWalker.ReachedState<String> state = walker.initState(StateID.of(String.class))) {
			assertEquals("hello", state.current());
			try (TransitionWalker.ReachedState<String> subState = state.initState(StateID.of("depends", String.class))) {
				assertEquals("hello world", subState.current());
			}
		}
		recording.end();
	}

	@Test
	public void initAsStateShouldWork() {
		recording.begin();
		Transitions baseRoutes = Transitions.from(
			Start.of(StateID.of(String.class), () -> State.of("hello", tearDownListener()))
		);

		TransitionWalker baseInit = baseRoutes.walker();

		Transitions transitions = Transitions.from(
			Start.of(StateID.of(String.class), () -> baseInit.initState(StateID.of(String.class)).asState()),
			Derive.of(StateID.of(String.class), StateID.of("depends", String.class),
				s -> State.of(s + " world", tearDownListener()))
		);

		TransitionWalker walker = transitions.walker();

		try (TransitionWalker.ReachedState<String> state = walker.initState(StateID.of(String.class))) {
			assertEquals("hello", state.current());
			try (TransitionWalker.ReachedState<String> subState = state.initState(StateID.of("depends", String.class))) {
				assertEquals("hello world", subState.current());
			}
		}
		recording.end();
	}

	@Test
	public void wrappedTransitions() {
		recording.begin();
		Transitions baseRoutes = Transitions.from(
			Start.of(StateID.of(String.class), () -> State.of("hello", tearDownListener()))
		);

		TransitionWalker baseInit = baseRoutes.walker();

		Transitions transitions = Transitions.from(
			baseInit.asTransitionTo(TransitionMapping.builder("hidden", StateID.of(String.class))
				.build()),
			Derive.of(StateID.of(String.class), StateID.of("depends", String.class),
				s -> State.of(s + " world", tearDownListener()))
		);

		TransitionWalker walker = transitions.walker();

		try (TransitionWalker.ReachedState<String> state = walker.initState(StateID.of(String.class))) {
			assertEquals("hello", state.current());
			try (TransitionWalker.ReachedState<String> subState = state.initState(StateID.of("depends", String.class))) {
				assertEquals("hello world", subState.current());
			}
		}

		recording.end();
		
		String dotFile = Transitions.edgeGraphAsDot("wrapped", transitions.asGraph());

		recording.output("app.dot", dotFile.replace("\t", "  "));
		recording.file("app.dot.svg", "HowToBuildAndUseTransitions.svg", GraphvizAdapter.asSvg(dotFile));
	}
}
