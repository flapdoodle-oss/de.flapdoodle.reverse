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

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

import de.flapdoodle.transition.NamedType;
import de.flapdoodle.transition.processlike.exceptions.RetryException;
import de.flapdoodle.transition.routes.Bridge;
import de.flapdoodle.transition.routes.End;
import de.flapdoodle.transition.routes.PartingWay;
import de.flapdoodle.transition.routes.Route;
import de.flapdoodle.transition.routes.SingleSource;
import de.flapdoodle.transition.routes.Start;
import de.flapdoodle.transition.types.Either;

public class ProcessEngineLikeTest {

	@Test
	public void simpleSample() {
		ProcessRoutes<SingleSource<?,?>> routes = ProcessRoutes.builder()
				.add(Start.of(typeOf(String.class)), () -> "12")
				.add(Bridge.of(typeOf(String.class), typeOf(Integer.class)), a -> Integer.valueOf(a))
				.add(End.of(typeOf(Integer.class)), i -> {})
				.build();

		ProcessEngineLike pe = ProcessEngineLike.with(routes);
		
		ProcessListener listener = new ProcessListener() {
			
			@Override
			public <T> void onStateChangeFailedWithRetry(Route<?> route, Optional<NamedType<T>> type, T state) {
				System.out.println("failed "+route+" -> "+type+"="+state);
			}
			
			@Override
			public <T> void onStateChange(Object oldState, NamedType<T> type, T newState) {
				System.out.println("changed "+oldState+" -> "+type+"="+newState);
			}
		};
		
		pe.run(listener);
		
	}
	
	@Test
	public void loopSample() {
		
		ProcessRoutes<SingleSource<?,?>> routes = ProcessRoutes.builder()
				.add(Start.of(typeOf("start", Integer.class)), () -> 0)
				.add(Bridge.of(typeOf("start", Integer.class), typeOf("decide", Integer.class)), a -> a+1)
				.add(PartingWay.of(typeOf("decide", Integer.class), typeOf("start", Integer.class), typeOf("end", Integer.class)), a -> a<3 ? Either.left(a) : Either.right(a))
				.add(End.of(typeOf("end", Integer.class)), i -> {})
				.build();
		
		ProcessEngineLike pe = ProcessEngineLike.with(routes);
		
		ProcessListener listener = new ProcessListener() {
			
			@Override
			public <T> void onStateChangeFailedWithRetry(Route<?> route, Optional<NamedType<T>> type, T state) {
				System.out.println("failed "+route+" -> "+type+"="+state);
			}
			
			@Override
			public <T> void onStateChange(Object oldState, NamedType<T> type, T newState) {
				System.out.println("changed "+oldState+" -> "+type+"="+newState);
			}
		};
		
		pe.run(listener);
	}
	
	@Test
	public void retrySample() {
		AtomicLong lastTimestamp=new AtomicLong(System.currentTimeMillis());
		
		ProcessRoutes<SingleSource<?,?>> routes = ProcessRoutes.builder()
				.add(Start.of(typeOf(String.class)), () -> "12")
				.add(Bridge.of(typeOf(String.class), typeOf(Integer.class)), a -> {
					long current=System.currentTimeMillis();
					long last = lastTimestamp.get();
					long diff = current-last;
					System.out.println("Diff: "+diff);
					if (diff<3) {
						throw new RetryException("diff is :"+diff);
					}
					lastTimestamp.set(current);
					return Integer.valueOf(a);
				})
				.add(End.of(typeOf(Integer.class)), i -> {})
				.build();

		ProcessEngineLike pe = ProcessEngineLike.with(routes);
		
		ProcessListener listener = new ProcessListener() {
			
			@Override
			public <T> void onStateChangeFailedWithRetry(Route<?> route, Optional<NamedType<T>> type, T state) {
				System.out.println("failed "+route+" -> "+type+"="+state);
				try {
					Thread.sleep(3);
				} catch (InterruptedException ix) {
					Thread.currentThread().interrupt();
				}
			}
			
			@Override
			public <T> void onStateChange(Object oldState, NamedType<T> type, T newState) {
				System.out.println("changed "+oldState+" -> "+type+"="+newState);
			}
		};
		
		pe.run(listener);
	}
	
	private static String asString(Object value) {
		return value!=null ? value+"("+value.getClass()+")" : "null";
	}

	private static <T> NamedType<T> typeOf(Class<T> type) {
		return NamedType.typeOf(type);
	}

	private static <T> NamedType<T> typeOf(String name, Class<T> type) {
		return NamedType.typeOf(name, type);
	}

}
