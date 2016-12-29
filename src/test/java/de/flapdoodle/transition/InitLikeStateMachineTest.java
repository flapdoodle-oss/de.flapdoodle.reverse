package de.flapdoodle.transition;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.flapdoodle.transition.routes.Routes;
import de.flapdoodle.transition.routes.Start;

public class InitLikeStateMachineTest {

	@Test
	public void startTransitionWorks() {
		Routes routes = Routes.builder()
			.add(Start.of(NamedType.of(String.class)), () -> State.of("hello"))
			.build();
		
		InitLikeStateMachine init = InitLikeStateMachine.with(routes);
		
		try (State<String> state = init.init(NamedType.of(String.class))) {
			assertEquals("hello",state.current());
		}
	}
}
