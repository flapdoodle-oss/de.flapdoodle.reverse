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
import de.flapdoodle.transition.routes.SingleDestination;

public class FluentInitRoutesBuilderTest {

	@Test
	public void buildRoutes() {
		InitRoutes<SingleDestination<?>> routes = InitRoutes.fluentBuilder()
				.start(String.class).with(() -> State.of("12", FluentInitRoutesBuilderTest::tearDown))
				.bridge(String.class, Integer.class).with(a -> State.of(Integer.valueOf(a), FluentInitRoutesBuilderTest::tearDown))
				.build();

		assertEquals(2, routes.all().size());
	}

	private static <T> NamedType<T> typeOf(Class<T> type) {
		return NamedType.typeOf(type);
	}

	private static <T> NamedType<T> typeOf(String name, Class<T> type) {
		return NamedType.typeOf(name, type);
	}

	private static <T> void tearDown(T value) {
		System.out.println("tear down '" + value + "'");
	}

}
