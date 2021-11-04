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

import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

import de.flapdoodle.transition.StateID;
import de.flapdoodle.transition.processlike.exceptions.RetryException;
import de.flapdoodle.transition.routes.Bridge;
import de.flapdoodle.transition.routes.End;
import de.flapdoodle.transition.routes.PartingWay;
import de.flapdoodle.transition.routes.HasSource;
import de.flapdoodle.transition.routes.Start;
import de.flapdoodle.types.Either;

public class ProcessEngineLikeTest {

	@Test
	public void simpleSample() {
		ProcessRoutes<HasSource<?, ?>> routes = ProcessRoutes.builder()
				.add(Start.of(StateID.of(String.class)), () -> "12")
				.add(Bridge.of(StateID.of(String.class), StateID.of(Integer.class)), a -> Integer.valueOf(a))
				.add(End.of(StateID.of(Integer.class)), i -> {
				})
				.build();

		ProcessEngineLike pe = ProcessEngineLike.with(routes);

		ProcessListener listener = ProcessListener.builder()
				.onStateChange((route, currentState) -> {
					System.out.println("failed " + route + " -> " + currentState);
				})
				.onStateChangeFailedWithRetry((oldState, newState) -> {
					System.out.println("changed " + oldState + " -> " + newState);
				})
				.build();

		pe.run(listener);

	}

	@Test
	public void loopSample() {

		ProcessRoutes<HasSource<?, ?>> routes = ProcessRoutes.builder()
				.add(Start.of(StateID.of("start", Integer.class)), () -> 0)
				.add(Bridge.of(StateID.of("start", Integer.class), StateID.of("decide", Integer.class)), a -> a + 1)
				.add(PartingWay.of(StateID.of("decide", Integer.class), StateID.of("start", Integer.class),
						StateID.of("end", Integer.class)), a -> a < 3 ? Either.left(a) : Either.right(a))
				.add(End.of(StateID.of("end", Integer.class)), i -> {
				})
				.build();

		ProcessEngineLike pe = ProcessEngineLike.with(routes);

		ProcessListener listener = ProcessListener.builder()
				.onStateChange((route, currentState) -> {
					System.out.println("failed " + route + " -> " + currentState);
				})
				.onStateChangeFailedWithRetry((oldState, newState) -> {
					System.out.println("changed " + oldState + " -> " + newState);
				})
				.build();


		pe.run(listener);
	}

	@Test
	public void retrySample() {
		AtomicLong lastTimestamp = new AtomicLong(System.currentTimeMillis());

		ProcessRoutes<HasSource<?, ?>> routes = ProcessRoutes.builder()
				.add(Start.of(StateID.of(String.class)), () -> "12")
				.add(Bridge.of(StateID.of(String.class), StateID.of(Integer.class)), a -> {
					long current = System.currentTimeMillis();
					long last = lastTimestamp.get();
					long diff = current - last;
					System.out.println("Diff: " + diff);
					if (diff < 3) {
						throw new RetryException("diff is :" + diff);
					}
					lastTimestamp.set(current);
					return Integer.valueOf(a);
				})
				.add(End.of(StateID.of(Integer.class)), i -> {
				})
				.build();

		ProcessEngineLike pe = ProcessEngineLike.with(routes);

		ProcessListener listener = ProcessListener.builder()
				.onStateChange((route, currentState) -> {
					System.out.println("failed " + route + " -> " + currentState);
				})
				.onStateChangeFailedWithRetry((oldState, newState) -> {
					System.out.println("changed " + oldState + " -> " + newState);
					try {
						Thread.sleep(3);
					} catch (InterruptedException ix) {
						Thread.currentThread().interrupt();
					}
				})
				.build();

		pe.run(listener);
	}

	private static String asString(Object value) {
		return value != null ? value + "(" + value.getClass() + ")" : "null";
	}

}
