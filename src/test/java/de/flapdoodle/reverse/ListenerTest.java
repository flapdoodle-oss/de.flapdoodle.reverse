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
package de.flapdoodle.reverse;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

public class ListenerTest {

	@Test
	public void initListenerHelper() {
		AtomicReference<String> container = new AtomicReference<String>();

		Listener listener = Listener.typedBuilder()
				.onStateReached(StateID.of(String.class), container::set)
				.onStateTearDown(StateID.of(String.class), container::set)
				.build();

		listener.onStateReached(StateID.of(String.class), "hello");
		assertEquals("hello", container.get());

		listener.onStateTearDown(StateID.of(String.class), "world");
		assertEquals("world", container.get());
	}
}
