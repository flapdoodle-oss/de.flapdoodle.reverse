package de.flapdoodle.reverse;

import de.flapdoodle.reverse.edges.Start;
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