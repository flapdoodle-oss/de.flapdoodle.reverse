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
package de.flapdoodle.transition.processlike;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.flapdoodle.transition.StateID;
import de.flapdoodle.transition.routes.Bridge;
import de.flapdoodle.transition.routes.End;
import de.flapdoodle.transition.routes.HasSource;
import de.flapdoodle.transition.routes.Start;

public class ProcessRoutesTest {

	@Test
	public void buildRoutes() {
		ProcessRoutes<HasSource<?, ?>> routes = ProcessRoutes.builder()
				.add(Start.of(StateID.of(String.class)), () -> "12")
				.add(Bridge.of(StateID.of(String.class), StateID.of(Integer.class)), a -> Integer.valueOf(a))
				.add(End.of(StateID.of(Integer.class)), i -> {
				})
				.build();

		assertEquals(3, routes.all().size());
	}
}
