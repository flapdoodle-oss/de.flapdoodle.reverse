package de.flapdoodle.transition.initlike;

import static de.flapdoodle.transition.NamedType.typeOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import de.flapdoodle.testdoc.Recorder;
import de.flapdoodle.testdoc.Recording;
import de.flapdoodle.testdoc.TabSize;
import de.flapdoodle.transition.State;
import de.flapdoodle.transition.TearDown;
import de.flapdoodle.transition.TearDownCounter;
import de.flapdoodle.transition.Try;
import de.flapdoodle.transition.routes.Bridge;
import de.flapdoodle.transition.routes.MergingJunction;
import de.flapdoodle.transition.routes.SingleDestination;
import de.flapdoodle.transition.routes.Start;
import de.flapdoodle.transition.routes.ThreeWayMergingJunction;

public class HowToTest {
	TearDownCounter tearDownCounter;

	@ClassRule
	public static Recording recording = Recorder.generateMarkDown("HowToBuildAnInitLikeSystem.md", TabSize.spaces(2));
	
	@Before
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
	public void minimalSample() {
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
	public void bridgeShouldWork() {
		InitRoutes<SingleDestination<?>> routes = InitRoutes.builder()
				.add(Start.of(typeOf(String.class)), () -> State.of("hello", tearDownListener()))
				.add(Bridge.of(typeOf(String.class), typeOf("bridge", String.class)), s -> State.of(s + " world", tearDownListener()))
				.build();

		InitLike init = InitLike.with(routes);

		try (InitLike.Init<String> state = init.init(typeOf("bridge", String.class))) {
			assertEquals("hello world", state.current());
		}

		assertTearDowns("hello world", "hello");
	}

	@Test
	public void mergingJunctionShouldWork() {
		InitRoutes<SingleDestination<?>> routes = InitRoutes.builder()
				.add(Start.of(typeOf("hello", String.class)), () -> State.of("hello", tearDownListener()))
				.add(Start.of(typeOf("again", String.class)), () -> State.of("again", tearDownListener()))
				.add(Bridge.of(typeOf("hello", String.class), typeOf("bridge", String.class)), s -> State.of("[" + s + "]", tearDownListener()))
				.add(MergingJunction.of(typeOf("bridge", String.class), typeOf("again", String.class), typeOf("merge", String.class)),
						(a, b) -> State.of(a + " " + b, tearDownListener()))
				.build();

		// String dotFile = RoutesAsGraph.routeGraphAsDot("dummy",
		// RoutesAsGraph.asGraph(routes.all()));
		// System.out.println("----------------------");
		// System.out.println(dotFile);
		// System.out.println("----------------------");

		InitLike init = InitLike.with(routes);

		try (InitLike.Init<String> state = init.init(typeOf("merge", String.class))) {
			assertEquals("[hello] again", state.current());
		}

		assertTearDowns("[hello] again", "[hello]", "hello", "again");
	}

	@Test
	public void threeWayMergingJunctionShouldWork() {
		InitRoutes<SingleDestination<?>> routes = InitRoutes.builder()
				.add(Start.of(typeOf("hello", String.class)), () -> State.of("hello", tearDownListener()))
				.add(Start.of(typeOf("again", String.class)), () -> State.of("again", tearDownListener()))
				.add(Bridge.of(typeOf("hello", String.class), typeOf("bridge", String.class)), s -> State.of("[" + s + "]", tearDownListener()))
				.add(ThreeWayMergingJunction.of(typeOf("hello", String.class), typeOf("bridge", String.class), typeOf("again", String.class),
						typeOf("3merge", String.class)), (a, b, c) -> State.of(a + " " + b + " " + c, tearDownListener()))
				.build();

		InitLike init = InitLike.with(routes);

		try (InitLike.Init<String> state = init.init(typeOf("3merge", String.class))) {
			assertEquals("hello [hello] again", state.current());
		}

		assertTearDowns("hello [hello] again", "[hello]", "hello", "again");
	}

	@Test
	public void twoDependencyTransitionWorks() {
		InitRoutes<SingleDestination<?>> routes = InitRoutes.builder()
				.add(Start.of(typeOf("a", String.class)), () -> State.of("hello", tearDownListener()))
				.add(Start.of(typeOf("b", String.class)), () -> State.of("world", tearDownListener()))
				.add(MergingJunction.of(typeOf("a", String.class), typeOf("b", String.class), typeOf(String.class)),
						(a, b) -> State.of(a + " " + b, tearDownListener()))
				.build();

		InitLike init = InitLike.with(routes);

		try (InitLike.Init<String> state = init.init(typeOf(String.class))) {
			assertEquals("hello world", state.current());
		}

		assertTearDowns("hello world", "hello", "world");
	}

	@Test
	public void multiUsageShouldTearDownAsLast() {
		InitRoutes<SingleDestination<?>> routes = InitRoutes.builder()
				.add(Start.of(typeOf("a", String.class)), () -> State.of("one", tearDownListener()))
				.add(Bridge.of(typeOf("a", String.class), typeOf("b", String.class)), a -> State.of("and " + a, tearDownListener()))
				.add(MergingJunction.of(typeOf("a", String.class), typeOf("b", String.class), typeOf(String.class)),
						(a, b) -> State.of(a + " " + b, tearDownListener()))
				.build();

		InitLike init = InitLike.with(routes);

		try (InitLike.Init<String> state = init.init(typeOf(String.class))) {
			assertEquals("one and one", state.current());
		}

		assertTearDowns("one and one", "and one", "one");
	}

	@Test
	public void tearDownShouldBeCalledOnRollback() {
		InitRoutes<SingleDestination<?>> routes = InitRoutes.builder()
				.add(Start.of(typeOf(String.class)), () -> State.of("hello", tearDownListener()))
				.add(Bridge.of(typeOf(String.class), typeOf("bridge", String.class)), s -> {
					if (true) {
						throw new RuntimeException("--error in transition--");
					}
					return State.of(s + " world", tearDownListener());
				})
				.build();

		InitLike init = InitLike.with(routes);

		assertException(() -> init.init(typeOf("bridge", String.class)), RuntimeException.class, "error on transition to NamedType(bridge:String), rollback");

		assertTearDowns("hello");
	}

	@Test
	public void localInitShouldWork() {
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
			assertTearDowns("hello world");
		}

		assertTearDowns("hello world", "hello");
	}

	@Test
	public void unknownInitShouldFail() {
		InitRoutes<SingleDestination<?>> routes = InitRoutes.builder()
				.add(Start.of(typeOf(String.class)), () -> State.of("foo"))
				.build();

		InitLike init = InitLike.with(routes);

		assertException(() -> init.init(typeOf("foo", String.class)), IllegalArgumentException.class,
				"state NamedType(foo:String) is not part of this init process");

		try (InitLike.Init<String> state = init.init(typeOf(String.class))) {
			assertEquals("foo", state.current());
			assertException(() -> state.init(typeOf(String.class)), IllegalArgumentException.class, "state NamedType(String) already initialized");
		}
	}

	@Test
	public void missingStartShouldFail() {
		InitRoutes<SingleDestination<?>> routes = InitRoutes.builder()
				.add(Bridge.of(typeOf(String.class), typeOf("bridge", String.class)), s -> State.of(s + " world", tearDownListener()))
				.build();

		InitLike init = InitLike.with(routes);

		assertException(() -> init.init(typeOf("bridge", String.class)), RuntimeException.class, "error on transition to NamedType(String), rollback");
	}

	private static void assertException(Supplier<?> supplier, Class<?> exceptionClass, String message) {
		try {
			supplier.get();
			fail("exception expected");
		}
		catch (RuntimeException rx) {
			assertEquals("exception class", exceptionClass, rx.getClass());
			assertEquals("exception message", message, rx.getLocalizedMessage());
		}
	}

}
