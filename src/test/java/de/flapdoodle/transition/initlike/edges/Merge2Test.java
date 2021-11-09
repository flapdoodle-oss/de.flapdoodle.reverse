package de.flapdoodle.transition.initlike.edges;

import de.flapdoodle.transition.StateID;
import de.flapdoodle.transition.initlike.State;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class Merge2Test {
		@Test
		public void fluentApiMustMapAttributes() {
				List<Merge2<Integer, Boolean, String>> variants = Arrays.asList(
						Merge2.given(Integer.class).and(Boolean.class).state(String.class).deriveBy((a,b)-> a+":"+b),
						Merge2.given(Integer.class).and(Boolean.class).state(String.class).with((a,b)-> State.of(a+":"+b))
				);

				assertThat(variants).allSatisfy(it -> {
						assertThat(it.left()).isEqualTo(StateID.of(Integer.class));
						assertThat(it.right()).isEqualTo(StateID.of(Boolean.class));
						assertThat(it.destination()).isEqualTo(StateID.of(String.class));
						assertThat(it.action().apply(1, true)).isEqualTo(State.of("1:true"));
				});
		}

}