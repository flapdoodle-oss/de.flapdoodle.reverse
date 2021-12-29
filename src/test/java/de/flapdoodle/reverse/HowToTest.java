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

import de.flapdoodle.reverse.edges.Derive;
import de.flapdoodle.reverse.edges.Join;
import de.flapdoodle.reverse.edges.Start;
import de.flapdoodle.testdoc.Recorder;
import de.flapdoodle.testdoc.Recording;
import de.flapdoodle.testdoc.TabSize;
import de.flapdoodle.types.Try;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class HowToTest {
	TearDownCounter tearDownCounter;

	@ClassRule
	public static Recording recording = Recorder.with("HowToBuildAndUseTransitions.md", TabSize.spaces(2));

	@Before
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
		derive = Derive.of(StateID.of("a", String.class), StateID.of("b", String.class), it -> State.of(it));
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
		List<Transition<?>> transitions = Arrays.asList(
			Start.of(StateID.of(String.class), () -> State.of("hello"))
		);

		TransitionWalker walker = TransitionWalker.with(transitions);

		try (TransitionWalker.ReachedState<String> state = walker.initState(StateID.of(String.class))) {
			assertEquals("hello", state.current());
		}

		recording.end();
	}

	@Test
	public void deriveShouldWork() {
		recording.begin();
		List<Transition<?>> transitions = Arrays.asList(
			Start.of(StateID.of(String.class), () -> State.of("hello")),
			Derive.of(StateID.of(String.class), StateID.of("depends", String.class), s -> State.of(s + " world"))
		);

		TransitionWalker walker = TransitionWalker.with(transitions);

		try (TransitionWalker.ReachedState<String> state = walker.initState(StateID.of("depends", String.class))) {
			assertEquals("hello world", state.current());
		}
		recording.end();
	}

	@Test
	public void joinShouldWork() {
		recording.begin();
		List<Transition<?>> transitions = Arrays.asList(
			Start.of(StateID.of("hello", String.class), () -> State.of("hello")),
			Start.of(StateID.of("again", String.class), () -> State.of("again")),
			Derive.of(StateID.of("hello", String.class), StateID.of("depends", String.class),
				s -> State.of("[" + s + "]")),

			Join.of(StateID.of("depends", String.class), StateID.of("again", String.class),
				StateID.of("merge", String.class),
				(a, b) -> State.of(a + " " + b))
		);

		TransitionWalker walker = TransitionWalker.with(transitions);

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

		List<Transition<?>> transitions = Arrays.asList(
			Start.of(StateID.of("hello", String.class), () -> State.of("hello")),
			Start.of(StateID.of("again", String.class), () -> State.of("again")),
			Derive.of(StateID.of("hello", String.class), StateID.of("depends", String.class),
				s -> State.of("[" + s + "]")),

			custom
		);

		TransitionWalker walker = TransitionWalker.with(transitions);

		try (TransitionWalker.ReachedState<String> state = walker.initState(StateID.of("custom", String.class))) {
			assertEquals("[hello] again", state.current());
		}
		recording.end();
	}

	@Test
	public void localInitShouldWork() {
		recording.begin();
		List<Transition<?>> transitions = Arrays.asList(
			Start.of(StateID.of(String.class), () -> State.of("hello", tearDownListener())),
			Derive.of(StateID.of(String.class), StateID.of("depends", String.class), s -> State.of(s + " world", tearDownListener()))
		);

		TransitionWalker walker = TransitionWalker.with(transitions);

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
		List<Transition<?>> baseRoutes = Arrays.asList(
			Start.of(StateID.of(String.class), () -> State.of("hello", tearDownListener()))
		);

		TransitionWalker baseInit = TransitionWalker.with(baseRoutes);

		List<Transition<?>> transitions = Arrays.asList(
			Start.of(StateID.of(String.class), () -> baseInit.initState(StateID.of(String.class)).asState()),
			Derive.of(StateID.of(String.class), StateID.of("depends", String.class),
				s -> State.of(s + " world", tearDownListener()))
		);

		TransitionWalker walker = TransitionWalker.with(transitions);

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
		List<Transition<?>> baseRoutes = Arrays.asList(
			Start.of(StateID.of(String.class), () -> State.of("hello", tearDownListener()))
		);

		TransitionWalker baseInit = TransitionWalker.with(baseRoutes);

		List<Transition<?>> transitions = Arrays.asList(
			baseInit.asTransitionTo(TransitionMapping.builder(StateID.of(String.class))
				.label("hidden")
				.build()),
			Derive.of(StateID.of(String.class), StateID.of("depends", String.class),
				s -> State.of(s + " world", tearDownListener()))
		);

		TransitionWalker walker = TransitionWalker.with(transitions);

		try (TransitionWalker.ReachedState<String> state = walker.initState(StateID.of(String.class))) {
			assertEquals("hello", state.current());
			try (TransitionWalker.ReachedState<String> subState = state.initState(StateID.of("depends", String.class))) {
				assertEquals("hello world", subState.current());
			}
		}

		String dotFile = Transitions.edgeGraphAsDot("wrapped", Transitions.asGraph(transitions));

		recording.end();

		recording.output("app.dot", dotFile.replace("\t", "  "));
	}

	/*
	 * sample app
	 */

	@Test
	public void createATempDir() {
		recording.begin();
		List<Transition<?>> transitions = Arrays.asList(
			Start.of(StateID.of(Path.class), () -> State.builder(Try
					.supplier(() -> Files.createTempDirectory("init-howto"))
					.mapCheckedException(RuntimeException::new)
					.get())
				.onTearDown(tempDir -> Try
					.consumer((Path p) -> Files.deleteIfExists(p))
					.mapCheckedException(RuntimeException::new)
					.accept(tempDir))
				.build())
		);

		TransitionWalker walker = TransitionWalker.with(transitions);

		recording.end();
		Path thisShouldBeDeleted;
		recording.begin();

		try (TransitionWalker.ReachedState<Path> state = walker.initState(StateID.of(Path.class))) {
			Path currentTempDir = state.current();
			recording.end();
			thisShouldBeDeleted = currentTempDir;
			assertNotNull(currentTempDir);
			recording.begin();
		}

		recording.end();
		assertFalse("tempDir deleted: " + thisShouldBeDeleted, thisShouldBeDeleted.toFile().exists());
	}

	@Test
	public void createAFileInTempDir() {
		recording.begin();
		StateID<Path> TEMP_DIR = StateID.of("tempDir", Path.class);
		StateID<Path> TEMP_FILE = StateID.of("tempFile", Path.class);

		List<Transition<?>> transitions = Arrays.asList(
			Start.of(TEMP_DIR, () -> State.builder(Try
					.supplier(() -> Files.createTempDirectory("init-howto"))
					.mapCheckedException(RuntimeException::new)
					.get())
				.onTearDown(tempDir -> Try.consumer((Path p) -> Files.deleteIfExists(p))
					.mapCheckedException(RuntimeException::new)
					.accept(tempDir))
				.build()),
			Derive.of(TEMP_DIR, TEMP_FILE, (Path tempDir) -> {
				Path tempFile = tempDir.resolve("test.txt");
				Try.consumer((Path t) -> Files.write(t, new byte[0]))
					.mapCheckedException(RuntimeException::new)
					.accept(tempFile);
				return State.builder(tempFile)
					.onTearDown(t -> Try.consumer((Path p) -> Files.deleteIfExists(p))
						.mapCheckedException(RuntimeException::new)
						.accept(t))
					.build();
			})
		);

		TransitionWalker walker = TransitionWalker.with(transitions);

		try (TransitionWalker.ReachedState<Path> state = walker.initState(TEMP_FILE)) {
			Path currentTempFile = state.current();
			recording.end();
			System.out.println(currentTempFile);
			assertNotNull(currentTempFile);
			assertTrue(currentTempFile.toFile().exists());
			recording.begin();
		}

		recording.end();
	}

	@Test
	public void writeContentIntoFileInTempDir() {
		recording.begin();
		StateID<Path> TEMP_DIR = StateID.of("tempDir", Path.class);
		StateID<Path> TEMP_FILE = StateID.of("tempFile", Path.class);
		StateID<String> CONTENT = StateID.of("content", String.class);

		List<Transition<?>> transitions = Arrays.asList(
			Start.of(TEMP_DIR, () -> State.builder(Try
					.supplier(() -> Files.createTempDirectory("init-howto"))
					.mapCheckedException(RuntimeException::new)
					.get())
				.onTearDown(tempDir -> Try
					.consumer((Path p) -> Files.deleteIfExists(p))
					.mapCheckedException(RuntimeException::new)
					.accept(tempDir))
				.build()),
			Derive.of(TEMP_DIR, TEMP_FILE, (Path tempDir) -> {
				Path tempFile = tempDir.resolve("test.txt");
				return State.builder(tempFile)
					.onTearDown(t -> Try
						.consumer((Path p) -> Files.deleteIfExists(p))
						.mapCheckedException(RuntimeException::new)
						.accept(t))
					.build();
			}),
			Start.of(CONTENT, () -> State.of("hello world")),
			Join.of(TEMP_FILE, CONTENT, StateID.of("done", Boolean.class), (tempFile, content) -> {
				Try
					.consumer((Path t) -> Files.write(t, "hello world".getBytes(Charset.defaultCharset())))
					.mapCheckedException(RuntimeException::new)
					.accept(tempFile);
				return State.of(true);
			})
		);

		TransitionWalker walker = TransitionWalker.with(transitions);

		try (TransitionWalker.ReachedState<Boolean> state = walker.initState(StateID.of("done", Boolean.class))) {
			Boolean done = state.current();
			assertTrue(done);
		}

		String dotFile = Transitions.edgeGraphAsDot("sample", Transitions.asGraph(transitions));

		recording.end();

		recording.output("app.dot", dotFile.replace("\t", "  "));
	}
}
