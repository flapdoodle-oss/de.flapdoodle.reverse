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

import static de.flapdoodle.transition.NamedType.typeOf;
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

import de.flapdoodle.testdoc.Includes;
import de.flapdoodle.testdoc.Recorder;
import de.flapdoodle.testdoc.Recording;
import de.flapdoodle.testdoc.TabSize;
import de.flapdoodle.transition.NamedType;
import de.flapdoodle.transition.TearDownCounter;
import de.flapdoodle.transition.Try;
import de.flapdoodle.transition.routes.Bridge;
import de.flapdoodle.transition.routes.MergingJunction;
import de.flapdoodle.transition.routes.RoutesAsGraph;
import de.flapdoodle.transition.routes.SingleDestination;
import de.flapdoodle.transition.routes.Start;
import de.flapdoodle.transition.routes.ThreeWayMergingJunction;

public class HowToTest {
	TearDownCounter tearDownCounter;

	@ClassRule
	public static Recording recording = Recorder.generateMarkDown("HowToBuildAnInitLikeSystem.md", TabSize.spaces(2))
		.sourceCodeOf("try", Try.class, Includes.WithoutImports, Includes.WithoutPackage, Includes.Trim);
	
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
		NamedType<String> stringType = NamedType.typeOf(String.class);
		NamedType<String> stringTypeWithLabel = NamedType.typeOf("foo", String.class);
		recording.end();
	}
	
	@Test
	public void edges() {
		recording.begin();
		Start<String> start;
		Bridge<String, String> bridge;
		MergingJunction<String, String, String> merge;
		ThreeWayMergingJunction<String, String, String, String> merge3;
		
		start = Start.of(typeOf(String.class));
		bridge = Bridge.of(typeOf("a", String.class), typeOf("b", String.class));
		merge = MergingJunction.of(typeOf("left",String.class), typeOf("right",String.class), typeOf("merged",String.class));
		merge3 = ThreeWayMergingJunction.of(typeOf("left",String.class), typeOf("middle",String.class), typeOf("right",String.class), typeOf("merged",String.class));
		recording.end();
	}
	
	@Test
	public void state() {
		recording.begin();
		State<String> state = State.builder("hello")
			.onTearDown(value -> System.out.println("tearDown "+value))
			.build();
		recording.end();
	}
	
	@Test
	public void startTransitionWorks() {
		recording.begin();
		InitRoutes<SingleDestination<?>> routes = InitRoutes.builder()
				.add(Start.of(typeOf(String.class)), () -> State.of("hello"))
				.build();

		InitLike init = InitLike.with(routes);

		try (InitLike.Init<String> state = init.init(typeOf(String.class))) {
			
			assertEquals("hello", state.current());
			
		}

		recording.end();
	}

	@Test
	public void bridgeShouldWork() {
		recording.begin();
		InitRoutes<SingleDestination<?>> routes = InitRoutes.builder()
				.add(Start.of(typeOf(String.class)), () -> State.of("hello"))
				.add(Bridge.of(typeOf(String.class), typeOf("bridge", String.class)), s -> State.of(s + " world"))
				.build();

		InitLike init = InitLike.with(routes);

		try (InitLike.Init<String> state = init.init(typeOf("bridge", String.class))) {
			
			assertEquals("hello world", state.current());
			
		}
		recording.end();
	}

	@Test
	public void mergingJunctionShouldWork() {
		recording.begin();
		InitRoutes<SingleDestination<?>> routes = InitRoutes.builder()
				.add(Start.of(typeOf("hello", String.class)), () -> State.of("hello"))
				.add(Start.of(typeOf("again", String.class)), () -> State.of("again"))
				.add(Bridge.of(typeOf("hello", String.class), typeOf("bridge", String.class)), s -> State.of("[" + s + "]"))
				.add(MergingJunction.of(typeOf("bridge", String.class), typeOf("again", String.class), typeOf("merge", String.class)),
						(a, b) -> State.of(a + " " + b))
				.build();

		InitLike init = InitLike.with(routes);

		try (InitLike.Init<String> state = init.init(typeOf("merge", String.class))) {
			
			assertEquals("[hello] again", state.current());
			
		}
		recording.end();
	}

	@Test
	public void threeWayMergingJunctionShouldWork() {
		recording.begin();
		InitRoutes<SingleDestination<?>> routes = InitRoutes.builder()
				.add(Start.of(typeOf("hello", String.class)), () -> State.of("hello"))
				.add(Start.of(typeOf("again", String.class)), () -> State.of("again"))
				.add(Bridge.of(typeOf("hello", String.class), typeOf("bridge", String.class)), s -> State.of("[" + s + "]"))
				.add(ThreeWayMergingJunction.of(typeOf("hello", String.class), typeOf("bridge", String.class), typeOf("again", String.class),
						typeOf("3merge", String.class)), (a, b, c) -> State.of(a + " " + b + " " + c))
				.build();

		InitLike init = InitLike.with(routes);

		try (InitLike.Init<String> state = init.init(typeOf("3merge", String.class))) {
			
			assertEquals("hello [hello] again", state.current());
			
		}
		recording.end();
	}

	@Test
	public void localInitShouldWork() {
		recording.begin();
		InitRoutes<SingleDestination<?>> routes = InitRoutes.builder()
				.add(Start.of(typeOf(String.class)), () -> State.of("hello", tearDownListener()))
				.add(Bridge.of(typeOf(String.class), typeOf("bridge", String.class)), s -> State.of(s + " world", tearDownListener()))
				.build();

		InitLike init = InitLike.with(routes);

		try (InitLike.Init<String> state = init.init(typeOf(String.class))) {
			
			assertEquals("hello", state.current());
			
			try (InitLike.Init<String> subState = state.init(typeOf("bridge", String.class))) {
				
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
				.add(Start.of(typeOf(Path.class)), () -> {
					return State.builder(Try.get(() -> Files.createTempDirectory("init-howto")))
							.onTearDown(tempDir -> Try.accept((Path p) -> Files.deleteIfExists(p), tempDir))
							.build();
				})
				.build();

		InitLike init = InitLike.with(routes);

		recording.end();
		Path thisShouldBeDeleted;
		recording.begin();
		
		try (InitLike.Init<Path> state = init.init(typeOf(Path.class))) {
			Path currentTempDir=state.current();
			recording.end();
			thisShouldBeDeleted=currentTempDir;
			assertNotNull(currentTempDir);
			recording.begin();
		}

		recording.end();
		assertFalse("tempDir deleted: "+thisShouldBeDeleted, thisShouldBeDeleted.toFile().exists());
	}
	
	@Test
	public void createAFileInTempDir() {
		recording.begin();
		NamedType<Path> TEMP_DIR = typeOf("tempDir",Path.class);
		NamedType<Path> TEMP_FILE = typeOf("tempFile",Path.class);
		
		InitRoutes<SingleDestination<?>> routes = InitRoutes.builder()
				.add(Start.of(TEMP_DIR), () -> {
					return State.builder(Try.get(() -> Files.createTempDirectory("init-howto")))
							.onTearDown(tempDir -> Try.accept((Path p) -> Files.deleteIfExists(p), tempDir))
							.build();
				})
				.add(Bridge.of(TEMP_DIR, TEMP_FILE), (Path tempDir) -> {
					Path tempFile = tempDir.resolve("test.txt");
					Try.accept(t -> Files.write(t, new byte[0]), tempFile);
					return State.builder(tempFile)
							.onTearDown(t -> Try.accept((Path p) -> Files.deleteIfExists(p), t))
							.build();
				})
				.build();

		InitLike init = InitLike.with(routes);

		try (InitLike.Init<Path> state = init.init(TEMP_FILE)) {
			Path currentTempFile=state.current();
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
		NamedType<Path> TEMP_DIR = typeOf("tempDir",Path.class);
		NamedType<Path> TEMP_FILE = typeOf("tempFile",Path.class);
		NamedType<String> CONTENT = typeOf("content", String.class);
		
		InitRoutes<SingleDestination<?>> routes = InitRoutes.builder()
				.add(Start.of(TEMP_DIR), () -> {
					return State.builder(Try.get(() -> Files.createTempDirectory("init-howto")))
							.onTearDown(tempDir -> Try.accept((Path p) -> Files.deleteIfExists(p), tempDir))
							.build();
				})
				.add(Bridge.of(TEMP_DIR, TEMP_FILE), (Path tempDir) -> {
					Path tempFile = tempDir.resolve("test.txt");
					return State.builder(tempFile)
							.onTearDown(t -> Try.accept((Path p) -> Files.deleteIfExists(p), t))
							.build();
				})
				.add(Start.of(CONTENT), () -> State.of("hello world"))
				.add(MergingJunction.of(TEMP_FILE, CONTENT, typeOf("done", Boolean.class)), (tempFile, content) -> {
					Try.accept(t -> Files.write(t, "hello world".getBytes(Charset.defaultCharset())), tempFile);
					return State.of(true);
				})
				.build();

		InitLike init = InitLike.with(routes);

		try (InitLike.Init<Boolean> state = init.init(typeOf("done", Boolean.class))) {
			Boolean done = state.current();
			assertTrue(done);
		}
		
		String dotFile = RoutesAsGraph.routeGraphAsDot("sampleApp", RoutesAsGraph.asGraph(routes.all()));
		recording.end();
		
		recording.output("app.dot", dotFile.replace("\t", "  "));
	}

	
}
