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

public class InitLikeStateMachineTest {

//	@Test
//	public void startTransitionWorks() {
//		TearDownCounter tearDownCounter = new TearDownCounter();
//		
//		Routes<Route<?>> routes = Routes.builder()
//			.add(Start.of(typeOf(String.class)), () -> State.of("hello", tearDownCounter.listener()))
//			.build();
//		
//		InitLikeStateMachine init = InitLikeStateMachine.with(routes.asWithSingleDestinations());
//		
//		try (AutocloseableState<String> state = init.init(typeOf(String.class))) {
//			assertEquals("hello",state.current());
//		}
//		
//		tearDownCounter.assertTearDowns("hello");
//	}
//	
//	@Test
//	public void bridgeShouldWork() {
//		TearDownCounter tearDownCounter = new TearDownCounter();
//		
//		Routes<Route<?>> routes = Routes.builder()
//				.add(Start.of(typeOf(String.class)), () -> State.of("hello", tearDownCounter.listener()))
//				.add(Bridge.of(typeOf(String.class), typeOf("bridge", String.class)), s -> s.map(h -> h+" world", tearDownCounter.listener()))
//				.build();
//			
//		InitLikeStateMachine init = InitLikeStateMachine.with(routes.asWithSingleDestinations());
//		
//		try (AutocloseableState<String> state = init.init(typeOf("bridge", String.class))) {
//			assertEquals("hello world",state.current());
//		}
//		
//		tearDownCounter.assertTearDowns("hello","hello world");
//	}
//	
//	@Test
//	public void mergingJunctionShouldWork() {
//		TearDownCounter tearDownCounter = new TearDownCounter();
//		
//		Routes<Route<?>> routes = Routes.builder()
//				.add(Start.of(typeOf("hello",String.class)), () -> State.of("hello", tearDownCounter.listener()))
//				.add(Start.of(typeOf("again",String.class)), () -> State.of("again", tearDownCounter.listener()))
//				.add(Bridge.of(typeOf("hello", String.class), typeOf("bridge", String.class)), s -> s.map(h -> "["+h+"]", tearDownCounter.listener()))
//				.add(MergingJunction.of(typeOf("bridge",String.class), typeOf("again",String.class), typeOf("merge",String.class)), (a,b) -> a.map(v -> v + " "+b.current(), tearDownCounter.listener()))
//				.build();
//		
//		String dotFile = RoutesAsGraph.routeGraphAsDot("dummy", RoutesAsGraph.asGraph(routes.all()));
//		System.out.println("----------------------");
//		System.out.println(dotFile);
//		System.out.println("----------------------");
//			
//		InitLikeStateMachine init = InitLikeStateMachine.with(routes.asWithSingleDestinations());
//		
//		try (AutocloseableState<String> state = init.init(typeOf("merge", String.class))) {
//			assertEquals("[hello] again",state.current());
//		}
//		
//		tearDownCounter.assertTearDowns("hello","[hello]","again","[hello] again");
//	}
//	
//	@Test
//	public void threeWayMergingJunctionShouldWork() {
//		TearDownCounter tearDownCounter = new TearDownCounter();
//		
//		Routes<Route<?>> routes = Routes.builder()
//				.add(Start.of(typeOf("hello",String.class)), () -> State.of("hello", tearDownCounter.listener()))
//				.add(Start.of(typeOf("again",String.class)), () -> State.of("again", tearDownCounter.listener()))
//				.add(Bridge.of(typeOf("hello", String.class), typeOf("bridge", String.class)), s -> s.map(h -> "["+h+"]", tearDownCounter.listener()))
//				.add(ThreeWayMergingJunction.of(typeOf("hello",String.class), typeOf("bridge",String.class), typeOf("again",String.class), typeOf("3merge",String.class)), (a,b,c) -> a.map(v -> v + " "+b.current()+" "+c.current(), tearDownCounter.listener()))
//				.build();
//		
//		String dotFile = RoutesAsGraph.routeGraphAsDot("dummy", RoutesAsGraph.asGraph(routes.all()));
//		System.out.println("----------------------");
//		System.out.println(dotFile);
//		System.out.println("----------------------");
//			
//		InitLikeStateMachine init = InitLikeStateMachine.with(routes.asWithSingleDestinations());
//		
//		try (AutocloseableState<String> state = init.init(typeOf("3merge", String.class))) {
//			assertEquals("hello [hello] again",state.current());
//		}
//		
//		tearDownCounter.assertTearDowns("hello","[hello]","again","[hello] again");
//	}
//	
//	private static <T> void tearDown(T value) {
//		System.out.println("tear down '"+value+"'");
//	}
//	
//	private static <T> NamedType<T> typeOf(Class<T> type) {
//		return NamedType.of(type);
//	}
//	
//	private static <T> NamedType<T> typeOf(String name, Class<T> type) {
//		return NamedType.of(name, type);
//	}
//	

}