/*
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

import de.flapdoodle.reverse.graph.TransitionGraph;
import de.flapdoodle.reverse.transitions.Derive;
import de.flapdoodle.reverse.transitions.Join;
import de.flapdoodle.reverse.transitions.Start;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class TransitionsTest {

	@Test
	public void exceptionOnCollision() {
		org.assertj.core.api.Assertions.assertThatThrownBy(() -> Transitions.from(
				Start.of(StateID.of(String.class), () -> State.of("hello")),
				Start.of(StateID.of(String.class), () -> State.of("foo"))
			)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("multiple transitions with same destination");
	}

	@Test
	public void exceptionIfReplaceDoesNotMatchAnyDestination() {
		Transitions transitions = Transitions.from(
			Start.of(StateID.of(String.class), () -> State.of("hello"))
		);

		org.assertj.core.api.Assertions.assertThatThrownBy(() -> transitions.replace(
				Start.of(StateID.of("foo", String.class), () -> State.of("hello"))
			)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("no transition with destination");
	}

	@Test
	public void graphAsDotMustGiveSameResultEachTimeCalledWithSameGraph() throws URISyntaxException, IOException {
		Transitions transitions = Transitions.from(
			Start.to(String.class).initializedWith("123"),
			Start.to(Integer.class).initializedWith(123),
			Derive.given(String.class).state(StateID.of("valueOf", Integer.class)).deriveBy(Integer::valueOf),
			Derive.given(Integer.class).state(StateID.of("toString", String.class)).deriveBy(Object::toString),
			Join.given(StateID.of("valueOf", Integer.class)).and(StateID.of("toString", String.class))
				.state(StateID.of("compare", Boolean.class)).deriveBy((i, s) -> Integer.valueOf(s).equals(i) && i.toString().equals(s))
		);

		String dotFile = TransitionGraph.edgeGraphAsDot("sample-dot", transitions);

		URL url = getClass().getResource("sample.dot");
		byte[] content = Files.readAllBytes(Paths.get(url.toURI()));
		String expectedDotFile = new String(content, StandardCharsets.UTF_8);
		
		assertThat(dotFile).isEqualTo(expectedDotFile);
	}
}