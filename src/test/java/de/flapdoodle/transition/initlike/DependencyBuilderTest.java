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

import de.flapdoodle.transition.routes.HasDestination;
import org.junit.Test;

public class DependencyBuilderTest {

	@Test
	public void buildRoutes() {
		InitRoutes<HasDestination<?>> routes = InitRoutes.builder()
				.state(String.class).isReachedBy(() -> State.of("12", DependencyBuilderTest::tearDown))
				.state(String.class).replace().isReachedBy(() -> State.of("13", DependencyBuilderTest::tearDown))
				.given(String.class).state(Integer.class)
				.isReachedBy(a -> State.of(Integer.valueOf(a), DependencyBuilderTest::tearDown))
				.build();

		assertEquals(2, routes.all().size());
	}

	private static <T> void tearDown(T value) {
		System.out.println("tear down '" + value + "'");
	}

}
