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

import de.flapdoodle.transition.StateID;
import de.flapdoodle.transition.TearDownCounter;
import de.flapdoodle.transition.initlike.edges.Depends;
import de.flapdoodle.transition.initlike.edges.Merge2;
import de.flapdoodle.transition.initlike.edges.Merge3;
import de.flapdoodle.transition.initlike.edges.Start;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

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
				List<Edge<?>> routes = Arrays.asList(
						Start.of(StateID.of(String.class), () -> State.of("hello", tearDownListener()))
				);

				InitLike init = InitLike.with(routes);

				try (InitLike.ReachedState<String> state = init.init(StateID.of(String.class))) {
						assertEquals("hello", state.current());
				}

				assertTearDowns("hello");
		}

		@Test
		public void startTransitionWithListenerWorks() {
				List<Edge<?>> routes = Arrays.asList(
						Start.of(StateID.of(String.class), () -> State.of("hello", tearDownListener()))
				);

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

				try (InitLike.ReachedState<String> state = init.init(StateID.of(String.class), listener)) {
						assertEquals("hello", state.current());
				}

				assertEquals("[up, down]", listenerCalled.toString());
				assertTearDowns("hello");
		}

		@Test
		public void bridgeShouldWork() {
				List<Edge<String>> routes = Arrays.asList(
						Start.of(StateID.of(String.class), () -> State.of("hello", tearDownListener())),
						Depends.of(StateID.of(String.class), StateID.of("bridge", String.class),
								s -> State.of(s + " world", tearDownListener()))
				);

				InitLike init = InitLike.with(routes);

				try (InitLike.ReachedState<String> state = init.init(StateID.of("bridge", String.class))) {
						assertEquals("hello world", state.current());
				}

				assertTearDowns("hello world", "hello");
		}

		@Test
		public void mergingJunctionShouldWork() {
				List<Edge<String>> routes = Arrays.asList(
						Start.of(StateID.of("hello", String.class), () -> State.of("hello", tearDownListener())),
						Start.of(StateID.of("again", String.class), () -> State.of("again", tearDownListener())),
						Depends.of(StateID.of("hello", String.class), StateID.of("bridge", String.class),
								s -> State.of("[" + s + "]", tearDownListener())),
						Merge2.of(StateID.of("bridge", String.class), StateID.of("again", String.class),
								StateID.of("merge", String.class),
								(a, b) -> State.of(a + " " + b, tearDownListener()))
				);

				// String dotFile = RoutesAsGraph.routeGraphAsDot("dummy",
				// RoutesAsGraph.asGraph(routes.all()));
				// System.out.println("----------------------");
				// System.out.println(dotFile);
				// System.out.println("----------------------");

				InitLike init = InitLike.with(routes);

				try (InitLike.ReachedState<String> state = init.init(StateID.of("merge", String.class))) {
						assertEquals("[hello] again", state.current());
				}

				assertTearDowns("[hello] again", "[hello]", "hello", "again");
		}

		@Test
		public void threeWayMergingJunctionShouldWork() {
				List<Edge<String>> routes = Arrays.asList(
						Start.of(StateID.of("hello", String.class), () -> State.of("hello", tearDownListener())),
						Start.of(StateID.of("again", String.class), () -> State.of("again", tearDownListener())),
						Depends.of(StateID.of("hello", String.class), StateID.of("bridge", String.class), s -> State.of("[" + s + "]", tearDownListener())),

						Merge3.of(StateID.of("hello", String.class), StateID.of("bridge", String.class),
								StateID.of("again", String.class),
								StateID.of("3merge", String.class),
								(a, b, c) -> State.of(a + " " + b + " " + c, tearDownListener()))
				);

				InitLike init = InitLike.with(routes);

				try (InitLike.ReachedState<String> state = init.init(StateID.of("3merge", String.class))) {
						assertEquals("hello [hello] again", state.current());
				}

				assertTearDowns("hello [hello] again", "[hello]", "hello", "again");
		}

		@Test
		public void twoDependencyTransitionWorks() {
				List<Edge<String>> routes = Arrays.asList(
						Start.of(StateID.of("a", String.class), () -> State.of("hello", tearDownListener())),
						Start.of(StateID.of("b", String.class), () -> State.of("world", tearDownListener())),
						Merge2.of(StateID.of("a", String.class), StateID.of("b", String.class),
								StateID.of(String.class),
								(a, b) -> State.of(a + " " + b, tearDownListener()))
				);

				InitLike init = InitLike.with(routes);

				try (InitLike.ReachedState<String> state = init.init(StateID.of(String.class))) {
						assertEquals("hello world", state.current());
				}

				assertTearDowns("hello world", "hello", "world");
		}

		@Test
		public void multiUsageShouldTearDownAsLast() {
				List<Edge<String>> routes = Arrays.asList(
						Start.of(StateID.of("a", String.class), () -> State.of("one", tearDownListener())),
						Depends.of(StateID.of("a", String.class), StateID.of("b", String.class),
								a -> State.of("and " + a, tearDownListener())),
						Merge2.of(StateID.of("a", String.class), StateID.of("b", String.class),
								StateID.of(String.class),
								(a, b) -> State.of(a + " " + b, tearDownListener()))
				);

				InitLike init = InitLike.with(routes);

				try (InitLike.ReachedState<String> state = init.init(StateID.of(String.class))) {
						assertEquals("one and one", state.current());
				}

				assertTearDowns("one and one", "and one", "one");
		}

		@Test
		public void tearDownShouldBeCalledOnRollback() {
				List<Edge<String>> routes = Arrays.asList(
						Start.of(StateID.of(String.class), () -> State.of("hello", tearDownListener())),
						Depends.of(StateID.of(String.class), StateID.of("bridge", String.class), s -> {
								if (true) {
										throw new RuntimeException("--error in transition--");
								}
								return State.of(s + " world", tearDownListener());
						})
				);

				InitLike init = InitLike.with(routes);

				assertException(() -> init.init(StateID.of("bridge", String.class)), RuntimeException.class,
						"error on transition to NamedType(bridge:String), rollback");

				assertTearDowns("hello");
		}

		@Test
		public void localInitShouldWork() {
				List<Edge<String>> routes = Arrays.asList(
						Start.of(StateID.of(String.class), () -> State.of("hello", tearDownListener())),
						Depends.of(StateID.of(String.class), StateID.of("bridge", String.class),
								s -> State.of(s + " world", tearDownListener()))
				);

				InitLike init = InitLike.with(routes);

				try (InitLike.ReachedState<String> state = init.init(StateID.of(String.class))) {
						assertEquals("hello", state.current());
						try (InitLike.ReachedState<String> subState = state.init(StateID.of("bridge", String.class))) {
								assertEquals("hello world", subState.current());
						}
						assertTearDowns("hello world");
				}

				assertTearDowns("hello world", "hello");
		}

		@Test
		public void cascadingInitShouldWork() {
				List<Edge<?>> baseRoutes = Arrays.asList(
						Start.of(StateID.of(String.class), () -> State.of("hello", tearDownListener()))
				);

				InitLike baseInit = InitLike.with(baseRoutes);

				List<Edge<?>> routes = Arrays.asList(
						Start.of(StateID.of(String.class), () -> baseInit.init(StateID.of(String.class)).asState()),
						Depends.of(StateID.of(String.class), StateID.of("bridge", String.class),
								s -> State.of(s + " world", tearDownListener()))
				);

				InitLike init = InitLike.with(routes);

				try (InitLike.ReachedState<String> state = init.init(StateID.of(String.class))) {
						assertEquals("hello", state.current());
						try (InitLike.ReachedState<String> subState = state.init(StateID.of("bridge", String.class))) {
								assertEquals("hello world", subState.current());
						}
						assertTearDowns("hello world");
				}

				assertTearDowns("hello world", "hello");
		}

		@Test
		public void unknownInitShouldFail() {
				List<Edge<?>> routes = Arrays.asList(
						Start.of(StateID.of(String.class), () -> State.of("foo"))
				);

				InitLike init = InitLike.with(routes);

				assertException(() -> init.init(StateID.of("foo", String.class)), IllegalArgumentException.class,
						"state NamedType(foo:String) is not part of this init process");

				try (InitLike.ReachedState<String> state = init.init(StateID.of(String.class))) {
						assertEquals("foo", state.current());
						assertException(() -> state.init(StateID.of(String.class)), IllegalArgumentException.class,
								"state NamedType(String) already initialized");
				}
		}

		@Test
		public void missingStartShouldFail() {
				List<Edge<?>> routes = Arrays.asList(
						Depends.of(StateID.of(String.class), StateID.of("bridge", String.class),
								s -> State.of(s + " world", tearDownListener()))
				);

				InitLike init = InitLike.with(routes);

				assertException(() -> init.init(StateID.of("bridge", String.class)), RuntimeException.class,
						"error on transition to NamedType(String), rollback");
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
