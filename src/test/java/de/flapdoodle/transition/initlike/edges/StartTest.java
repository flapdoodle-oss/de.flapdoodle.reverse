package de.flapdoodle.transition.initlike.edges;

import de.flapdoodle.transition.StateID;
import de.flapdoodle.transition.initlike.State;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StartTest {

		@Test
		public void fluentApiMustMapAttributes() {
				List<Start<String>> variants = Arrays.asList(
						Start.to(String.class).initializedWith("some value"),
						Start.to(String.class).providedBy(() -> "some value"),
						Start.to(String.class).with(() -> State.of("some value"))
				);

				assertThat(variants).allSatisfy(it -> {
						assertThat(it.destination()).isEqualTo(StateID.of(String.class));
						assertThat(it.action().get()).isEqualTo(State.of("some value"));
				});
		}
}