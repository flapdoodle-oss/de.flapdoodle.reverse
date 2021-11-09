package de.flapdoodle.transition.initlike.edges;

import de.flapdoodle.transition.StateID;
import de.flapdoodle.transition.initlike.State;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DependsTest {
		@Test
		public void fluentApiMustMapAttributes() {
				List<Depends<Integer, String>> variants = Arrays.asList(
						Depends.given(Integer.class).state(String.class).deriveBy(it-> "["+it+"]"),
						Depends.given(Integer.class).state(String.class).with(it-> State.of("["+it+"]"))
				);

				assertThat(variants).allSatisfy(it -> {
						assertThat(it.source()).isEqualTo(StateID.of(Integer.class));
						assertThat(it.destination()).isEqualTo(StateID.of(String.class));
						assertThat(it.action().apply(1)).isEqualTo(State.of("[1]"));
				});
		}
}