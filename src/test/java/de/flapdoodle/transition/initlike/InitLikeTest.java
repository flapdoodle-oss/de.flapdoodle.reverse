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
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;

import de.flapdoodle.transition.NamedType;
import de.flapdoodle.transition.TearDownCounter;
import de.flapdoodle.transition.routes.Bridge;
import de.flapdoodle.transition.routes.Merge3Junction;
import de.flapdoodle.transition.routes.MergingJunction;
import de.flapdoodle.transition.routes.SingleDestination;
import de.flapdoodle.transition.routes.Start;

public class InitLikeTest {

	TearDownCounter tearDownCounter;

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
	public void startTransitionWorks() {
		InitRoutes<SingleDestination<?>> routes = InitRoutes.builder()
				.add(Start.of(typeOf(String.class)), () -> State.of("hello", tearDownListener()))
				.build();

		InitLike init = InitLike.with(routes);

		try (InitLike.Init<String> state = init.init(typeOf(String.class))) {
			assertEquals("hello", state.current());
		}

		assertTearDowns("hello");
	}

	@Test
	public void startTransitionWithListenerWorks() {
		InitRoutes<SingleDestination<?>> routes = InitRoutes.builder()
				.add(Start.of(typeOf(String.class)), () -> State.of("hello", tearDownListener()))
				.build();

		InitLike init = InitLike.with(routes);
		List<String> listenerCalled=new ArrayList<>();

		InitListener listener=InitListener.builder()
				.onStateReached((type, value) -> {
					assertEquals(NamedType.typeOf(String.class),type);
					assertEquals("hello",value);
					listenerCalled.add("up");
				})
				.onTearDown((type, value) -> {
					assertEquals(NamedType.typeOf(String.class),type);
					assertEquals("hello",value);
					listenerCalled.add("down");
				})
				.build();

		try (InitLike.Init<String> state = init.init(typeOf(String.class), listener)) {
			assertEquals("hello", state.current());
		}

		assertEquals("[up, down]", listenerCalled.toString());
		assertTearDowns("hello");
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
				.add(Merge3Junction.of(typeOf("hello", String.class), typeOf("bridge", String.class), typeOf("again", String.class),
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
	public void cascadingInitShouldWork() {
		InitRoutes<SingleDestination<?>> baseRoutes = InitRoutes.builder()
				.add(Start.of(typeOf(String.class)), () -> State.of("hello", tearDownListener()))
				.build();

		InitLike baseInit = InitLike.with(baseRoutes);

		InitRoutes<SingleDestination<?>> routes = InitRoutes.builder()
				.add(Start.of(typeOf(String.class)), () -> baseInit.init(typeOf(String.class)).asState())
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
