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

import org.junit.Test;

import de.flapdoodle.transition.NamedType;
import de.flapdoodle.transition.State;
import de.flapdoodle.transition.TearDownCounter;
import de.flapdoodle.transition.routes.MergingJunction;
import de.flapdoodle.transition.routes.Route;
import de.flapdoodle.transition.routes.Routes;
import de.flapdoodle.transition.routes.Start;

public class InitLikeTest {
	@Test
	public void startTransitionWorks() {
		TearDownCounter tearDownCounter = new TearDownCounter();
		
		Routes<Route<?>> routes = Routes.builder()
			.add(Start.of(typeOf(String.class)), () -> State.of("hello", tearDownCounter.listener()))
			.build();
		
		InitLike init = InitLike.with(routes.asWithSingleDestinations());
		
		try (InitLike.Init<String> state = init.init(typeOf(String.class))) {
			assertEquals("hello",state.current());
		}
		
		tearDownCounter.assertTearDowns("hello");
	}

	@Test
	public void twoDependencyTransitionWorks() {
		TearDownCounter tearDownCounter = new TearDownCounter();
		
		Routes<Route<?>> routes = Routes.builder()
				.add(Start.of(typeOf("a",String.class)), () -> State.of("hello", tearDownCounter.listener()))
				.add(Start.of(typeOf("b",String.class)), () -> State.of("world", tearDownCounter.listener()))
				.add(MergingJunction.of(typeOf("a",String.class), typeOf("b",String.class), typeOf(String.class)), (a,b) -> State.of(a.current()+" "+b.current(), tearDownCounter.listener()))
			.build();
		
		InitLike init = InitLike.with(routes.asWithSingleDestinations());
		
		try (InitLike.Init<String> state = init.init(typeOf(String.class))) {
			assertEquals("hello world",state.current());
		}
		
		tearDownCounter.assertTearDowns("hello","world","hello world");
	}
	
	private static <T> void tearDown(T value) {
		System.out.println("tear down '"+value+"'");
	}
	
	private static <T> NamedType<T> typeOf(Class<T> type) {
		return NamedType.of(type);
	}
	
	private static <T> NamedType<T> typeOf(String name, Class<T> type) {
		return NamedType.of(name, type);
	}

}
