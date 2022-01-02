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

import de.flapdoodle.reverse.transitions.Start;
import org.junit.jupiter.api.Test;

class TransitionsTest {

	@Test
	public void exceptionOnCollision() {
		org.assertj.core.api.Assertions.assertThatThrownBy(() -> Transitions.empty().addAll(
				Start.of(StateID.of(String.class), () -> State.of("hello")),
				Start.of(StateID.of(String.class), () -> State.of("foo"))
			)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("multiple transitions with same destination");
	}

	@Test
	public void exceptionIfReplaceDoesNotMatchAnyDestination() {
		Transitions transitions = Transitions.empty().addAll(
			Start.of(StateID.of(String.class), () -> State.of("hello"))
		);

		org.assertj.core.api.Assertions.assertThatThrownBy(() -> transitions.replace(
				Start.of(StateID.of("foo", String.class), () -> State.of("hello"))
			)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("no transition with destination");
	}
}