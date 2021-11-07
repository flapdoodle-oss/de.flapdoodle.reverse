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

import de.flapdoodle.transition.StateID;
import de.flapdoodle.transition.process.Edge;
import de.flapdoodle.transition.process.ProcessEngine;
import de.flapdoodle.transition.process.State;
import de.flapdoodle.transition.process.edges.Conditional;
import de.flapdoodle.transition.process.edges.End;
import de.flapdoodle.transition.process.edges.Start;
import de.flapdoodle.transition.process.edges.Step;
import de.flapdoodle.types.Either;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

public class ProcessEngineTest {

		@Test
		public void simpleSample() {
				AtomicReference<Integer> endValue = new AtomicReference<>();

				List<Edge> edges = Arrays.asList(
						Start.of(StateID.of(String.class), () -> "12"),
						Step.of(StateID.of(String.class), StateID.of(Integer.class), a -> Integer.valueOf(a)),
						End.of(StateID.of(Integer.class), endValue::set)
				);

				ProcessEngine pe = ProcessEngine.with(edges);
				ProcessEngine.Started started = pe.start();

				assertThat(started.currentState())
						.isEqualTo(de.flapdoodle.transition.process.State.of(StateID.of(String.class), "12"));

				assertThat(started.next()).isTrue();

				assertThat(started.currentState())
						.isEqualTo(de.flapdoodle.transition.process.State.of(StateID.of(Integer.class), 12));

				assertThat(started.next()).isFalse();

				assertThat(endValue.get()).isEqualTo(12);
		}

		@Test
		public void loopSample() {
				AtomicReference<Integer> endValue = new AtomicReference<>();

				StateID<Integer> startID = StateID.of("start", Integer.class);
				StateID<Integer> decideID = StateID.of("decide", Integer.class);
				StateID<Integer> endID = StateID.of("end", Integer.class);

				List<Edge> edges = Arrays.asList(
						Start.of(startID, () -> 0),
						Step.of(startID, decideID, a -> a + 1),
						Conditional.of(decideID, startID, endID, a -> a < 2 ? Either.left(a) : Either.right(a)),
						End.of(endID, endValue::set)
				);

				ProcessEngine pe = ProcessEngine.with(edges);
				ProcessEngine.Started started = pe.start();

				assertThat(started.currentState())
						.isEqualTo(de.flapdoodle.transition.process.State.of(startID, 0));

				assertThat(started.next()).isTrue();

				assertThat(started.currentState())
						.isEqualTo(de.flapdoodle.transition.process.State.of(decideID, 1));

				assertThat(started.next()).isTrue();

				assertThat(started.currentState())
						.isEqualTo(de.flapdoodle.transition.process.State.of(startID, 1));

				assertThat(started.next()).isTrue();

				assertThat(started.currentState())
						.isEqualTo(de.flapdoodle.transition.process.State.of(decideID, 2));

				assertThat(started.next()).isTrue();

				assertThat(started.currentState())
						.isEqualTo(State.of(endID, 2));

				assertThat(started.next()).isFalse();

				assertThat(endValue.get()).isEqualTo(2);
		}

		@Test(expected = IllegalArgumentException.class)
		public void missingConnection() {
				AtomicReference<Integer> endValue = new AtomicReference<>();

				List<Edge> edges = Arrays.asList(
						Start.of(StateID.of("a",String.class), () -> "12"),
						Step.of(StateID.of("b",String.class), StateID.of(Integer.class), a -> Integer.valueOf(a)),
						End.of(StateID.of(Integer.class), endValue::set)
				);

				ProcessEngine.with(edges);
		}

}
