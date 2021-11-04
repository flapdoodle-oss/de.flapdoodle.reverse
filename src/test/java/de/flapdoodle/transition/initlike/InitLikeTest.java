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
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;

import de.flapdoodle.transition.StateID;
import de.flapdoodle.transition.TearDownCounter;
import de.flapdoodle.transition.routes.Bridge;
import de.flapdoodle.transition.routes.Merge3Junction;
import de.flapdoodle.transition.routes.MergingJunction;
import de.flapdoodle.transition.routes.HasDestination;
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
		InitRoutes<HasDestination<?>> routes = InitRoutes.rawBuilder()
				.add(Start.of(StateID.of(String.class)), () -> State.of("hello", tearDownListener()))
				.build();

		InitLike init = InitLike.with(routes);

		try (InitLike.Init<String> state = init.init(StateID.of(String.class))) {
			assertEquals("hello", state.current());
		}

		assertTearDowns("hello");
	}

	@Test
	public void startTransitionWithListenerWorks() {
		InitRoutes<HasDestination<?>> routes = InitRoutes.rawBuilder()
				.add(Start.of(StateID.of(String.class)), () -> State.of("hello", tearDownListener()))
				.build();

		InitLike init = InitLike.with(routes);
		List<String> listenerCalled = new ArrayList<>();

		InitListener listener = InitListener.builder()
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

		try (InitLike.Init<String> state = init.init(StateID.of(String.class), listener)) {
			assertEquals("hello", state.current());
		}

		assertEquals("[up, down]", listenerCalled.toString());
		assertTearDowns("hello");
	}

	@Test
	public void bridgeShouldWork() {
		InitRoutes<HasDestination<?>> routes = InitRoutes.rawBuilder()
				.add(Start.of(StateID.of(String.class)), () -> State.of("hello", tearDownListener()))
				.add(Bridge.of(StateID.of(String.class), StateID.of("bridge", String.class)),
						s -> State.of(s + " world", tearDownListener()))
				.build();

		InitLike init = InitLike.with(routes);

		try (InitLike.Init<String> state = init.init(StateID.of("bridge", String.class))) {
			assertEquals("hello world", state.current());
		}

		assertTearDowns("hello world", "hello");
	}

	@Test
	public void mergingJunctionShouldWork() {
		InitRoutes<HasDestination<?>> routes = InitRoutes.rawBuilder()
				.add(Start.of(StateID.of("hello", String.class)), () -> State.of("hello", tearDownListener()))
				.add(Start.of(StateID.of("again", String.class)), () -> State.of("again", tearDownListener()))
				.add(Bridge.of(StateID.of("hello", String.class), StateID.of("bridge", String.class)),
						s -> State.of("[" + s + "]", tearDownListener()))
				.add(
						MergingJunction.of(StateID.of("bridge", String.class), StateID.of("again", String.class),
								StateID.of("merge", String.class)),
						(a, b) -> State.of(a + " " + b, tearDownListener()))
				.build();

		// String dotFile = RoutesAsGraph.routeGraphAsDot("dummy",
		// RoutesAsGraph.asGraph(routes.all()));
		// System.out.println("----------------------");
		// System.out.println(dotFile);
		// System.out.println("----------------------");

		InitLike init = InitLike.with(routes);

		try (InitLike.Init<String> state = init.init(StateID.of("merge", String.class))) {
			assertEquals("[hello] again", state.current());
		}

		assertTearDowns("[hello] again", "[hello]", "hello", "again");
	}

	@Test
	public void threeWayMergingJunctionShouldWork() {
		InitRoutes<HasDestination<?>> routes = InitRoutes.rawBuilder()
				.add(Start.of(StateID.of("hello", String.class)), () -> State.of("hello", tearDownListener()))
				.add(Start.of(StateID.of("again", String.class)), () -> State.of("again", tearDownListener()))
				.add(Bridge.of(StateID.of("hello", String.class), StateID.of("bridge", String.class)),
						s -> State.of("[" + s + "]", tearDownListener()))
				.add(
						Merge3Junction.of(StateID.of("hello", String.class), StateID.of("bridge", String.class),
								StateID.of("again", String.class),
								StateID.of("3merge", String.class)),
						(a, b, c) -> State.of(a + " " + b + " " + c, tearDownListener()))
				.build();

		InitLike init = InitLike.with(routes);

		try (InitLike.Init<String> state = init.init(StateID.of("3merge", String.class))) {
			assertEquals("hello [hello] again", state.current());
		}

		assertTearDowns("hello [hello] again", "[hello]", "hello", "again");
	}

	@Test
	public void twoDependencyTransitionWorks() {
		InitRoutes<HasDestination<?>> routes = InitRoutes.rawBuilder()
				.add(Start.of(StateID.of("a", String.class)), () -> State.of("hello", tearDownListener()))
				.add(Start.of(StateID.of("b", String.class)), () -> State.of("world", tearDownListener()))
				.add(
						MergingJunction.of(StateID.of("a", String.class), StateID.of("b", String.class),
								StateID.of(String.class)),
						(a, b) -> State.of(a + " " + b, tearDownListener()))
				.build();

		InitLike init = InitLike.with(routes);

		try (InitLike.Init<String> state = init.init(StateID.of(String.class))) {
			assertEquals("hello world", state.current());
		}

		assertTearDowns("hello world", "hello", "world");
	}

	@Test
	public void multiUsageShouldTearDownAsLast() {
		InitRoutes<HasDestination<?>> routes = InitRoutes.rawBuilder()
				.add(Start.of(StateID.of("a", String.class)), () -> State.of("one", tearDownListener()))
				.add(Bridge.of(StateID.of("a", String.class), StateID.of("b", String.class)),
						a -> State.of("and " + a, tearDownListener()))
				.add(
						MergingJunction.of(StateID.of("a", String.class), StateID.of("b", String.class),
								StateID.of(String.class)),
						(a, b) -> State.of(a + " " + b, tearDownListener()))
				.build();

		InitLike init = InitLike.with(routes);

		try (InitLike.Init<String> state = init.init(StateID.of(String.class))) {
			assertEquals("one and one", state.current());
		}

		assertTearDowns("one and one", "and one", "one");
	}

	@Test
	public void tearDownShouldBeCalledOnRollback() {
		InitRoutes<HasDestination<?>> routes = InitRoutes.rawBuilder()
				.add(Start.of(StateID.of(String.class)), () -> State.of("hello", tearDownListener()))
				.add(Bridge.of(StateID.of(String.class), StateID.of("bridge", String.class)), s -> {
					if (true) {
						throw new RuntimeException("--error in transition--");
					}
					return State.of(s + " world", tearDownListener());
				})
				.build();

		InitLike init = InitLike.with(routes);

		assertException(() -> init.init(StateID.of("bridge", String.class)), RuntimeException.class,
				"error on transition to NamedType(bridge:String), rollback");

		assertTearDowns("hello");
	}

	@Test
	public void localInitShouldWork() {
		InitRoutes<HasDestination<?>> routes = InitRoutes.rawBuilder()
				.add(Start.of(StateID.of(String.class)), () -> State.of("hello", tearDownListener()))
				.add(Bridge.of(StateID.of(String.class), StateID.of("bridge", String.class)),
						s -> State.of(s + " world", tearDownListener()))
				.build();

		InitLike init = InitLike.with(routes);

		try (InitLike.Init<String> state = init.init(StateID.of(String.class))) {
			assertEquals("hello", state.current());
			try (InitLike.Init<String> subState = state.init(StateID.of("bridge", String.class))) {
				assertEquals("hello world", subState.current());
			}
			assertTearDowns("hello world");
		}

		assertTearDowns("hello world", "hello");
	}

	@Test
	public void cascadingInitShouldWork() {
		InitRoutes<HasDestination<?>> baseRoutes = InitRoutes.rawBuilder()
				.add(Start.of(StateID.of(String.class)), () -> State.of("hello", tearDownListener()))
				.build();

		InitLike baseInit = InitLike.with(baseRoutes);

		InitRoutes<HasDestination<?>> routes = InitRoutes.rawBuilder()
				.add(Start.of(StateID.of(String.class)), () -> baseInit.init(StateID.of(String.class)).asState())
				.add(Bridge.of(StateID.of(String.class), StateID.of("bridge", String.class)),
						s -> State.of(s + " world", tearDownListener()))
				.build();

		InitLike init = InitLike.with(routes);

		try (InitLike.Init<String> state = init.init(StateID.of(String.class))) {
			assertEquals("hello", state.current());
			try (InitLike.Init<String> subState = state.init(StateID.of("bridge", String.class))) {
				assertEquals("hello world", subState.current());
			}
			assertTearDowns("hello world");
		}

		assertTearDowns("hello world", "hello");
	}

	@Test
	public void unknownInitShouldFail() {
		InitRoutes<HasDestination<?>> routes = InitRoutes.rawBuilder()
				.add(Start.of(StateID.of(String.class)), () -> State.of("foo"))
				.build();

		InitLike init = InitLike.with(routes);

		assertException(() -> init.init(StateID.of("foo", String.class)), IllegalArgumentException.class,
				"state NamedType(foo:String) is not part of this init process");

		try (InitLike.Init<String> state = init.init(StateID.of(String.class))) {
			assertEquals("foo", state.current());
			assertException(() -> state.init(StateID.of(String.class)), IllegalArgumentException.class,
					"state NamedType(String) already initialized");
		}
	}

	@Test
	public void missingStartShouldFail() {
		InitRoutes<HasDestination<?>> routes = InitRoutes.rawBuilder()
				.add(Bridge.of(StateID.of(String.class), StateID.of("bridge", String.class)),
						s -> State.of(s + " world", tearDownListener()))
				.build();

		InitLike init = InitLike.with(routes);

		assertException(() -> init.init(StateID.of("bridge", String.class)), RuntimeException.class,
				"error on transition to NamedType(String), rollback");
	}

	private static void assertException(Supplier<?> supplier, Class<?> exceptionClass, String message) {
		try {
			supplier.get();
			fail("exception expected");
		} catch (RuntimeException rx) {
			assertEquals("exception class", exceptionClass, rx.getClass());
			assertEquals("exception message", message, rx.getLocalizedMessage());
		}
	}

}
