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
package de.flapdoodle.transition.initlike;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import de.flapdoodle.testdoc.Recorder;
import de.flapdoodle.testdoc.Recording;
import de.flapdoodle.testdoc.TabSize;
import de.flapdoodle.transition.StateID;
import de.flapdoodle.transition.TearDownCounter;
import de.flapdoodle.transition.routes.Bridge;
import de.flapdoodle.transition.routes.Merge3Junction;
import de.flapdoodle.transition.routes.MergingJunction;
import de.flapdoodle.transition.routes.RoutesAsGraph;
import de.flapdoodle.transition.routes.SingleDestination;
import de.flapdoodle.transition.routes.Start;
import de.flapdoodle.types.Try;

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
		Bridge<String, String> bridge;
		MergingJunction<String, String, String> merge;
		Merge3Junction<String, String, String, String> merge3;

		start = Start.of(StateID.of(String.class));
		bridge = Bridge.of(StateID.of("a", String.class), StateID.of("b", String.class));
		merge = MergingJunction.of(StateID.of("left", String.class), StateID.of("right", String.class),
				StateID.of("merged", String.class));
		merge3 = Merge3Junction.of(StateID.of("left", String.class), StateID.of("middle", String.class),
				StateID.of("right", String.class), StateID.of("merged", String.class));
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
		InitRoutes<SingleDestination<?>> routes = InitRoutes.rawBuilder()
				.add(Start.of(StateID.of(String.class)), () -> State.of("hello"))
				.build();

		InitLike init = InitLike.with(routes);

		try (InitLike.Init<String> state = init.init(StateID.of(String.class))) {

			assertEquals("hello", state.current());

		}

		recording.end();
	}

	@Test
	public void startTransitionFluentWorks() {
		recording.begin();
		InitRoutes<SingleDestination<?>> routes = InitRoutes.builder()
				.state(String.class).isInitializedWith("hello")
				.build();

		InitLike init = InitLike.with(routes);

		try (InitLike.Init<String> state = init.init(StateID.of(String.class))) {

			assertEquals("hello", state.current());

		}

		recording.end();
	}

	@Test
	public void bridgeShouldWork() {
		recording.begin();
		InitRoutes<SingleDestination<?>> routes = InitRoutes.rawBuilder()
				.add(Start.of(StateID.of(String.class)), () -> State.of("hello"))
				.add(Bridge.of(StateID.of(String.class), StateID.of("bridge", String.class)), s -> State.of(s + " world"))
				.build();

		InitLike init = InitLike.with(routes);

		try (InitLike.Init<String> state = init.init(StateID.of("bridge", String.class))) {

			assertEquals("hello world", state.current());

		}
		recording.end();
	}

	@Test
	public void bridgeFluentShouldWork() {
		recording.begin();
		InitRoutes<SingleDestination<?>> routes = InitRoutes.builder()
				.state(String.class).isInitializedWith("hello")
				.given(String.class).state(StateID.of("bridge", String.class)).isDerivedBy(s -> s + " world")
				.build();

		InitLike init = InitLike.with(routes);

		try (InitLike.Init<String> state = init.init(StateID.of("bridge", String.class))) {

			assertEquals("hello world", state.current());

		}
		recording.end();
	}

	@Test
	public void mergingJunctionShouldWork() {
		recording.begin();
		InitRoutes<SingleDestination<?>> routes = InitRoutes.rawBuilder()
				.add(Start.of(StateID.of("hello", String.class)), () -> State.of("hello"))
				.add(Start.of(StateID.of("again", String.class)), () -> State.of("again"))
				.add(Bridge.of(StateID.of("hello", String.class), StateID.of("bridge", String.class)),
						s -> State.of("[" + s + "]"))
				.add(
						MergingJunction.of(StateID.of("bridge", String.class), StateID.of("again", String.class),
								StateID.of("merge", String.class)),
						(a, b) -> State.of(a + " " + b))
				.build();

		InitLike init = InitLike.with(routes);

		try (InitLike.Init<String> state = init.init(StateID.of("merge", String.class))) {

			assertEquals("[hello] again", state.current());

		}
		recording.end();
	}

	@Test
	public void mergingJunctionFluentShouldWork() {
		recording.begin();
		StateID<String> hello = StateID.of("hello", String.class);
		StateID<String> again = StateID.of("again", String.class);
		StateID<String> mappedHello = StateID.of("mapped", String.class);
		StateID<String> result = StateID.of("result", String.class);

		InitRoutes<SingleDestination<?>> routes = InitRoutes.builder()
				.state(hello).isInitializedWith("hello")
				.state(again).isInitializedWith("again")
				.given(hello).state(mappedHello).isDerivedBy(s -> "[" + s + "]")
				.given(mappedHello, again).state(result)
				.isDerivedBy((a, b) -> a + " " + b)
				.build();

		InitLike init = InitLike.with(routes);

		try (InitLike.Init<String> state = init.init(result)) {

			assertEquals("[hello] again", state.current());

		}
		recording.end();
	}

	@Test
	public void threeWayMergingJunctionShouldWork() {
		recording.begin();
		InitRoutes<SingleDestination<?>> routes = InitRoutes.rawBuilder()
				.add(Start.of(StateID.of("hello", String.class)), () -> State.of("hello"))
				.add(Start.of(StateID.of("again", String.class)), () -> State.of("again"))
				.add(Bridge.of(StateID.of("hello", String.class), StateID.of("bridge", String.class)),
						s -> State.of("[" + s + "]"))
				.add(Merge3Junction.of(StateID.of("hello", String.class), StateID.of("bridge", String.class),
						StateID.of("again", String.class),
						StateID.of("3merge", String.class)), (a, b, c) -> State.of(a + " " + b + " " + c))
				.build();

		InitLike init = InitLike.with(routes);

		try (InitLike.Init<String> state = init.init(StateID.of("3merge", String.class))) {

			assertEquals("hello [hello] again", state.current());

		}
		recording.end();
	}

	@Test
	public void threeWayMergingJunctionFluentShouldWork() {
		recording.begin();
		StateID<String> hello = StateID.of("hello", String.class);
		StateID<String> again = StateID.of("again", String.class);
		StateID<String> mapped = StateID.of("mapped", String.class);
		StateID<String> result = StateID.of("result", String.class);

		InitRoutes<SingleDestination<?>> routes = InitRoutes.builder()
				.state(hello).isInitializedWith("hello")
				.state(again).isInitializedWith("again")
				.given(hello).state(mapped).isDerivedBy(s -> "[" + s + "]")
				.given(hello, mapped, again).state(result)
				.isReachedBy((a, b, c) -> State.of(a + " " + b + " " + c))
				.build();

		InitLike init = InitLike.with(routes);

		try (InitLike.Init<String> state = init.init(result)) {

			assertEquals("hello [hello] again", state.current());

		}
		recording.end();
	}

	@Test
	public void localInitShouldWork() {
		recording.begin();
		InitRoutes<SingleDestination<?>> routes = InitRoutes.builder()
				.state(String.class).isReachedBy(() -> State.of("hello", tearDownListener()))
				.given(String.class).state(StateID.of("bridge", String.class))
				.isReachedBy(s -> State.of(s + " world", tearDownListener()))
				.build();

		InitLike init = InitLike.with(routes);

		try (InitLike.Init<String> state = init.init(StateID.of(String.class))) {

			assertEquals("hello", state.current());

			try (InitLike.Init<String> subState = state.init(StateID.of("bridge", String.class))) {

				assertEquals("hello world", subState.current());

			}
		}
		recording.end();
	}

	@Test
	public void initAsStateShouldWork() {
		recording.begin();
		InitRoutes<SingleDestination<?>> baseRoutes = InitRoutes.builder()
				.state(String.class).isReachedBy(() -> State.of("hello", tearDownListener()))
				.build();

		InitLike baseInit = InitLike.with(baseRoutes);

		InitRoutes<SingleDestination<?>> routes = InitRoutes.builder()
				.state(String.class).isReachedBy(() -> baseInit.init(StateID.of(String.class)).asState())
				.given(String.class).state(StateID.of("bridge", String.class))
				.isReachedBy(s -> State.of(s + " world", tearDownListener()))
				.build();

		InitLike init = InitLike.with(routes);

		try (InitLike.Init<String> state = init.init(StateID.of(String.class))) {

			assertEquals("hello", state.current());

			try (InitLike.Init<String> subState = state.init(StateID.of("bridge", String.class))) {

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
		InitRoutes<SingleDestination<?>> routes = InitRoutes.builder()
				.state(Path.class).isReachedBy(() -> {
					return State.builder(Try
							.supplier(() -> Files.createTempDirectory("init-howto"))
							.mapCheckedException(RuntimeException::new)
							.get())
							.onTearDown(tempDir -> Try
									.consumer((Path p) -> Files.deleteIfExists(p))
									.mapCheckedException(RuntimeException::new)
									.accept(tempDir))
							.build();
				})
				.build();

		InitLike init = InitLike.with(routes);

		recording.end();
		Path thisShouldBeDeleted;
		recording.begin();

		try (InitLike.Init<Path> state = init.init(StateID.of(Path.class))) {
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

		InitRoutes<SingleDestination<?>> routes = InitRoutes.builder()
				.state(TEMP_DIR).isReachedBy(() -> {
					return State.builder(Try
							.supplier(() -> Files.createTempDirectory("init-howto"))
							.mapCheckedException(RuntimeException::new)
							.get())
							.onTearDown(tempDir -> Try.consumer((Path p) -> Files.deleteIfExists(p))
									.mapCheckedException(RuntimeException::new)
									.accept(tempDir))
							.build();
				})
				.given(TEMP_DIR).state(TEMP_FILE).isReachedBy((Path tempDir) -> {
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
				.build();

		InitLike init = InitLike.with(routes);

		try (InitLike.Init<Path> state = init.init(TEMP_FILE)) {
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

		InitRoutes<SingleDestination<?>> routes = InitRoutes.builder()
				.state(TEMP_DIR).isReachedBy(() -> {
					return State.builder(Try
							.supplier(() -> Files.createTempDirectory("init-howto"))
							.mapCheckedException(RuntimeException::new)
							.get())
							.onTearDown(tempDir -> Try
									.consumer((Path p) -> Files.deleteIfExists(p))
									.mapCheckedException(RuntimeException::new)
									.accept(tempDir))
							.build();
				})
				.given(TEMP_DIR).state(TEMP_FILE).isReachedBy((Path tempDir) -> {
					Path tempFile = tempDir.resolve("test.txt");
					return State.builder(tempFile)
							.onTearDown(t -> Try
									.consumer((Path p) -> Files.deleteIfExists(p))
									.mapCheckedException(RuntimeException::new)
									.accept(t))
							.build();
				})
				.state(CONTENT).isInitializedWith("hello world")
				.given(TEMP_FILE, CONTENT).state(StateID.of("done", Boolean.class)).isReachedBy((tempFile, content) -> {
					Try
							.consumer((Path t) -> Files.write(t, "hello world".getBytes(Charset.defaultCharset())))
							.mapCheckedException(RuntimeException::new)
							.accept(tempFile);
					return State.of(true);
				})
				.build();

		InitLike init = InitLike.with(routes);

		try (InitLike.Init<Boolean> state = init.init(StateID.of("done", Boolean.class))) {
			Boolean done = state.current();
			assertTrue(done);
		}

		String dotFile = RoutesAsGraph.routeGraphAsDot("sampleApp",
				RoutesAsGraph.asGraphIncludingStartAndEnd(routes.all()));
		recording.end();

		recording.output("app.dot", dotFile.replace("\t", "  "));
	}

}
