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
package de.flapdoodle.transition.init;

import de.flapdoodle.testdoc.Recorder;
import de.flapdoodle.testdoc.Recording;
import de.flapdoodle.testdoc.TabSize;
import de.flapdoodle.transition.StateID;
import de.flapdoodle.transition.TearDownCounter;
import de.flapdoodle.transition.init.edges.Depends;
import de.flapdoodle.transition.init.edges.Merge2;
import de.flapdoodle.transition.init.edges.Merge3;
import de.flapdoodle.transition.init.edges.Start;
import de.flapdoodle.types.Try;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class HowToTest {
		TearDownCounter tearDownCounter;

		@ClassRule
		public static Recording recording = Recorder.with("HowToBuildAnInitLikeSystem.md", TabSize.spaces(2));

		@Before
		public final void before() {
				tearDownCounter = new TearDownCounter();
		}

		private TearDown<String> tearDownListener() {
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
		public void edges() {
				recording.begin();
				Start<String> start;
				Depends<String, String> bridge;
				Merge2<String, String, String> merge;
				Merge3<String, String, String, String> merge3;

				start = Start.of(StateID.of(String.class), () -> "");
				bridge = Depends.of(StateID.of("a", String.class), StateID.of("b", String.class), it -> it);
				merge = Merge2.of(StateID.of("left", String.class), StateID.of("right", String.class),
						StateID.of("merged", String.class), (a, b) -> a + b);
				merge3 = Merge3.of(StateID.of("left", String.class), StateID.of("middle", String.class),
						StateID.of("right", String.class), StateID.of("merged", String.class), (a, b, c) -> a + b + c);
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
				List<Edge<?>> routes = Arrays.asList(
						Start.with(StateID.of(String.class), () -> State.of("hello"))
				);

				Init init = Init.with(routes);

				try (Init.ReachedState<String> state = init.init(StateID.of(String.class))) {
						assertEquals("hello", state.current());
				}

				recording.end();
		}

		@Test
		public void bridgeShouldWork() {
				recording.begin();
				List<Edge<?>> routes = Arrays.asList(
						Start.with(StateID.of(String.class), () -> State.of("hello")),
						Depends.with(StateID.of(String.class), StateID.of("bridge", String.class), s -> State.of(s + " world"))
				);

				Init init = Init.with(routes);

				try (Init.ReachedState<String> state = init.init(StateID.of("bridge", String.class))) {
						assertEquals("hello world", state.current());
				}
				recording.end();
		}

		@Test
		public void mergingJunctionShouldWork() {
				recording.begin();
				List<Edge<?>> routes = Arrays.asList(
						Start.with(StateID.of("hello", String.class), () -> State.of("hello")),
						Start.with(StateID.of("again", String.class), () -> State.of("again")),
						Depends.with(StateID.of("hello", String.class), StateID.of("bridge", String.class),
								s -> State.of("[" + s + "]")),

						Merge2.with(StateID.of("bridge", String.class), StateID.of("again", String.class),
								StateID.of("merge", String.class),
								(a, b) -> State.of(a + " " + b))
				);

				Init init = Init.with(routes);

				try (Init.ReachedState<String> state = init.init(StateID.of("merge", String.class))) {
						assertEquals("[hello] again", state.current());
				}
				recording.end();
		}

		@Test
		public void threeWayMergingJunctionShouldWork() {
				recording.begin();
				List<Edge<?>> routes = Arrays.asList(
						Start.with(StateID.of("hello", String.class), () -> State.of("hello")),
						Start.with(StateID.of("again", String.class), () -> State.of("again")),
						Depends.with(StateID.of("hello", String.class), StateID.of("bridge", String.class),
								s -> State.of("[" + s + "]")),
						Merge3.with(StateID.of("hello", String.class), StateID.of("bridge", String.class),
								StateID.of("again", String.class),
								StateID.of("3merge", String.class), (a, b, c) -> State.of(a + " " + b + " " + c))
				);

				Init init = Init.with(routes);

				try (Init.ReachedState<String> state = init.init(StateID.of("3merge", String.class))) {
						assertEquals("hello [hello] again", state.current());
				}
				recording.end();
		}

		@Test
		public void localInitShouldWork() {
				recording.begin();
				List<Edge<?>> routes = Arrays.asList(
						Start.with(StateID.of(String.class), () -> State.of("hello", tearDownListener())),
						Depends.with(StateID.of(String.class), StateID.of("bridge", String.class), s -> State.of(s + " world", tearDownListener()))
				);

				Init init = Init.with(routes);

				try (Init.ReachedState<String> state = init.init(StateID.of(String.class))) {
						assertEquals("hello", state.current());
						try (Init.ReachedState<String> subState = state.init(StateID.of("bridge", String.class))) {
								assertEquals("hello world", subState.current());
						}
				}
				recording.end();
		}

		@Test
		public void initAsStateShouldWork() {
				recording.begin();
				List<Edge<?>> baseRoutes = Arrays.asList(
						Start.with(StateID.of(String.class), () -> State.of("hello", tearDownListener()))
				);

				Init baseInit = Init.with(baseRoutes);

				List<Edge<?>> routes = Arrays.asList(
						Start.with(StateID.of(String.class), () -> baseInit.init(StateID.of(String.class)).asState()),
						Depends.with(StateID.of(String.class), StateID.of("bridge", String.class),
								s -> State.of(s + " world", tearDownListener()))
				);

				Init init = Init.with(routes);

				try (Init.ReachedState<String> state = init.init(StateID.of(String.class))) {
						assertEquals("hello", state.current());
						try (Init.ReachedState<String> subState = state.init(StateID.of("bridge", String.class))) {
								assertEquals("hello world", subState.current());
						}
				}
				recording.end();
		}
		/*
		 * sample app
		 */

		@Test
		public void createATempDir() {
				recording.begin();
				List<Edge<?>> routes = Arrays.asList(
						Start.with(StateID.of(Path.class), () -> State.builder(Try
										.supplier(() -> Files.createTempDirectory("init-howto"))
										.mapCheckedException(RuntimeException::new)
										.get())
								.onTearDown(tempDir -> Try
										.consumer((Path p) -> Files.deleteIfExists(p))
										.mapCheckedException(RuntimeException::new)
										.accept(tempDir))
								.build())
				);

				Init init = Init.with(routes);

				recording.end();
				Path thisShouldBeDeleted;
				recording.begin();

				try (Init.ReachedState<Path> state = init.init(StateID.of(Path.class))) {
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

				List<Edge<?>> routes = Arrays.asList(
						Start.with(TEMP_DIR, () -> State.builder(Try
										.supplier(() -> Files.createTempDirectory("init-howto"))
										.mapCheckedException(RuntimeException::new)
										.get())
								.onTearDown(tempDir -> Try.consumer((Path p) -> Files.deleteIfExists(p))
										.mapCheckedException(RuntimeException::new)
										.accept(tempDir))
								.build()),
						Depends.with(TEMP_DIR, TEMP_FILE, (Path tempDir) -> {
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

				Init init = Init.with(routes);

				try (Init.ReachedState<Path> state = init.init(TEMP_FILE)) {
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

				List<Edge<?>> routes = Arrays.asList(
						Start.with(TEMP_DIR, () -> State.builder(Try
										.supplier(() -> Files.createTempDirectory("init-howto"))
										.mapCheckedException(RuntimeException::new)
										.get())
								.onTearDown(tempDir -> Try
										.consumer((Path p) -> Files.deleteIfExists(p))
										.mapCheckedException(RuntimeException::new)
										.accept(tempDir))
								.build()),
						Depends.with(TEMP_DIR, TEMP_FILE, (Path tempDir) -> {
								Path tempFile = tempDir.resolve("test.txt");
								return State.builder(tempFile)
										.onTearDown(t -> Try
												.consumer((Path p) -> Files.deleteIfExists(p))
												.mapCheckedException(RuntimeException::new)
												.accept(t))
										.build();
						}),
						Start.with(CONTENT, () -> State.of("hello world")),
						Merge2.with(TEMP_FILE, CONTENT, StateID.of("done", Boolean.class), (tempFile, content) -> {
								Try
										.consumer((Path t) -> Files.write(t, "hello world".getBytes(Charset.defaultCharset())))
										.mapCheckedException(RuntimeException::new)
										.accept(tempFile);
								return State.of(true);
						})
				);

				Init init = Init.with(routes);

				try (Init.ReachedState<Boolean> state = init.init(StateID.of("done", Boolean.class))) {
						Boolean done = state.current();
						assertTrue(done);
				}

				String dotFile = EdgesAsGraph.edgeGraphAsDot("sampleApp",
						EdgesAsGraph.asGraphIncludingStartAndEnd(routes));
				recording.end();

				recording.output("app.dot", dotFile.replace("\t", "  "));
		}
}
