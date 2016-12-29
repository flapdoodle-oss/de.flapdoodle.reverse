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
package de.flapdoodle.transition.routes;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.flapdoodle.transition.NamedType;
import de.flapdoodle.transition.State;

public class RoutesTest {

	@Test
	public void buildRoutes() {
		Routes<Route<?>> routes = Routes.builder()
			.add(Start.of(typeOf(String.class)), () -> State.of("12",RoutesTest::tearDown))
			.add(Bridge.of(typeOf(String.class), typeOf(Integer.class)), a -> a.map(Integer::valueOf, RoutesTest::tearDown))
			.build();
		
		assertEquals(2,routes.all().size());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void routesContainingPartingWayCanNotBeHandledAsSingleDestination() {
		Routes<SingleDestination<?>> routes = Routes.builder()
			.add(Start.of(typeOf(String.class)), () -> State.of("12",RoutesTest::tearDown))
			.add(PartingWay.of(typeOf(String.class), typeOf("left", String.class), typeOf("right", String.class)), s -> Either.left(s))
			.add(Bridge.of(typeOf(String.class), typeOf(Integer.class)), a -> a.map(Integer::valueOf, RoutesTest::tearDown))
			.build()
			.asWithSingleDestinations();
	}
	
	private static <T> NamedType<T> typeOf(Class<T> type) {
		return NamedType.of(type);
	}
	
	private static <T> NamedType<T> typeOf(String name, Class<T> type) {
		return NamedType.of(name, type);
	}
	
	private static <T> void tearDown(T value) {
		System.out.println("tear down '"+value+"'");
	}

}
