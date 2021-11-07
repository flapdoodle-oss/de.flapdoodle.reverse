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
package de.flapdoodle.transition.process;

import de.flapdoodle.testdoc.Recorder;
import de.flapdoodle.testdoc.Recording;
import de.flapdoodle.testdoc.TabSize;
import de.flapdoodle.transition.StateID;
import de.flapdoodle.transition.process.edges.Conditional;
import de.flapdoodle.transition.process.edges.End;
import de.flapdoodle.transition.process.edges.Start;
import de.flapdoodle.transition.process.edges.Step;
import de.flapdoodle.types.Either;
import org.assertj.core.api.Assertions;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

public class HowToTest {
	@ClassRule
	public static Recording recording = Recorder.with("HowToBuildAnProcessEngine.md", TabSize.spaces(2));

	@Test
	public void vertex() {
		recording.begin();
		StateID<String> id = StateID.of(String.class);
		StateID<String> idWithLabel = StateID.of("foo", String.class);
		recording.end();
	}

	@Test
	public void edges() {
		recording.begin();
		Start<String> start;
		Step<String, String> bridge;
		Conditional<String, String, String> parting;
		End<String> end;

		start = Start.of(StateID.of(String.class),() -> "");
		bridge = Step.of(StateID.of("a", String.class), StateID.of("b", String.class), it -> it);
		parting = Conditional.of(StateID.of("start", String.class), StateID.of("oneDestination", String.class),
				StateID.of("otherDestination", String.class), it -> Either.left(it));
		end = End.of(StateID.of("start", String.class), it -> {});
		recording.end();
	}

	@Test
	public void state() {
		recording.begin();
		de.flapdoodle.transition.processlike.State<String> state = de.flapdoodle.transition.processlike.State.of(StateID.of("foo", String.class), "hello");
		recording.end();
	}

	@Test
	public void startAndEnd() {
		AtomicReference<String> result = new AtomicReference<>();
		List<State<?>> states = new ArrayList<>();

		recording.begin();
			List<Edge> routes = Arrays.asList(
					Start.of(StateID.of(String.class), () -> "foo"),
					End.of(StateID.of(String.class), i -> {
							result.set(i);
					})
			);

		ProcessEngine pe = ProcessEngine.with(routes);
		ProcessEngine.Started started = pe.start();
		do {
				states.add(started.currentState());
		} while (started.next());

		recording.end();

		assertEquals("foo", result.get());
		assertEquals(1, states.size());
		assertEquals("foo", states.get(0).value());
	}

	@Test
	public void startBridgeAndEnd() {
		AtomicReference<Integer> result = new AtomicReference<>();
		recording.begin();
			List<Edge> routes = Arrays.asList(
					Start.of(StateID.of(String.class), () -> "12"),
					Step.of(StateID.of(String.class), StateID.of(Integer.class), Integer::valueOf),
					End.of(StateID.of(Integer.class), i -> {
							result.set(i);
					})
			);

		ProcessEngine pe = ProcessEngine.with(routes);
		pe.start().forEach(state -> {
				// called for each new state
		});
		recording.end();

		assertEquals(Integer.valueOf(12), result.get());
	}

	@Test
	public void loopSample() {
		List<Object> values = new ArrayList<>();

		recording.begin();
			List<Edge> routes = Arrays.asList(
				Start.of(StateID.of("start", Integer.class), () -> 0),
				Step.of(StateID.of("start", Integer.class), StateID.of("decide", Integer.class), a -> a + 1),
				Conditional.of(StateID.of("decide", Integer.class), StateID.of("start", Integer.class),
						StateID.of("end", Integer.class), a -> a < 3 ? Either.left(a) : Either.right(a)),
				End.of(StateID.of("end", Integer.class), values::add)
				);

		ProcessEngine pe = ProcessEngine.with(routes);
		pe.start().forEach(currentState -> {
				if (currentState.type().name().equals("decide")) {
						values.add(currentState.value());
				}
		});

		String dot = "";//RoutesAsGraph.routeGraphAsDot("simpleLoop", RoutesAsGraph.asGraphIncludingStartAndEnd(routes.all()));
		recording.end();

		recording.output("dotFile", dot);

		assertEquals("[1, 2, 3, 3]", values.toString());
	}
}
