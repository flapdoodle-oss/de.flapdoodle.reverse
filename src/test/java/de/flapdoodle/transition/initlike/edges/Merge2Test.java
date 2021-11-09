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