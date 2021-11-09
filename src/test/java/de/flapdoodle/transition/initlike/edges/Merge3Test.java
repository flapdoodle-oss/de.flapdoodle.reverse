package de.flapdoodle.transition.initlike.edges;

import de.flapdoodle.transition.StateID;
import de.flapdoodle.transition.initlike.State;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class Merge3Test {
		@Test
		public void fluentApiMustMapAttributes() {
				List<Merge3<Integer, Boolean, Double, String>> variants = Arrays.asList(
						Merge3.given(Integer.class).and(Boolean.class).and(Double.class).state(String.class).deriveBy((a,b,c)-> a+":"+b+":"+c),
						Merge3.given(Integer.class).and(Boolean.class).and(Double.class).state(String.class).with((a,b,c)-> State.of(a+":"+b+":"+c))
				);

				assertThat(variants).allSatisfy(it -> {
						assertThat(it.left()).isEqualTo(StateID.of(Integer.class));
						assertThat(it.middle()).isEqualTo(StateID.of(Boolean.class));
						assertThat(it.right()).isEqualTo(StateID.of(Double.class));
						assertThat(it.destination()).isEqualTo(StateID.of(String.class));
						assertThat(it.action().apply(1, true, 0.2)).isEqualTo(State.of("1:true:0.2"));
				});
		}

}