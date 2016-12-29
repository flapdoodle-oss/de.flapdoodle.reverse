package de.flapdoodle.transition.initlike;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.flapdoodle.transition.NamedType;
import de.flapdoodle.transition.State;
import de.flapdoodle.transition.TearDownCounter;
import de.flapdoodle.transition.routes.Bridge;
import de.flapdoodle.transition.routes.Route;
import de.flapdoodle.transition.routes.Routes;
import de.flapdoodle.transition.routes.Start;

public class InitLikeStateMachineTest {

	@Test
	public void startTransitionWorks() {
		TearDownCounter tearDownCounter = new TearDownCounter();
		
		Routes<Route<?>> routes = Routes.builder()
			.add(Start.of(typeOf(String.class)), () -> State.of("hello", tearDownCounter.listener()))
			.build();
		
		InitLikeStateMachine init = InitLikeStateMachine.with(routes.asWithSingleDestinations());
		
		try (AutocloseableState<String> state = init.init(typeOf(String.class))) {
			assertEquals("hello",state.current());
		}
		
		tearDownCounter.assertTearDowns("hello");
	}
	
	@Test
	public void bridgeShouldWork() {
		TearDownCounter tearDownCounter = new TearDownCounter();
		
		Routes<Route<?>> routes = Routes.builder()
				.add(Start.of(typeOf(String.class)), () -> State.of("hello", tearDownCounter.listener()))
				.add(Bridge.of(typeOf(String.class), typeOf("bridge", String.class)), s -> s.map(h -> h+" world", tearDownCounter.listener()))
				.build();
			
		InitLikeStateMachine init = InitLikeStateMachine.with(routes.asWithSingleDestinations());
		
		try (AutocloseableState<String> state = init.init(typeOf("bridge", String.class))) {
			assertEquals("hello world",state.current());
		}
		
		tearDownCounter.assertTearDowns("hello","hello world");
	}
	
	private static <T> void tearDown(T value) {
		System.out.println("tear down '"+value+"'");
	}
	
	private static <T> NamedType<T> typeOf(Class<T> type) {
		return NamedType.of(type);
	}
	
	private static <T> NamedType<T> typeOf(String name, Class<T> type) {
		return NamedType.of(name, type);
	}
	

}
