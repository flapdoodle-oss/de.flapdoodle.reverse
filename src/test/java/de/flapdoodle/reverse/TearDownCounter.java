/*
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
package de.flapdoodle.reverse;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class TearDownCounter {

	Map<Object, RuntimeException> tearDowns = new LinkedHashMap<>();

	public <T> TearDown<T> listener() {
		return t -> {
			RuntimeException old = tearDowns.put(t, new RuntimeException("->" + t));
			if (old != null) {
				old.printStackTrace();
				throw new IllegalArgumentException("tearDown for [" + t + "] already called");
			}
		};
	}

	public void assertTearDowns(Object... values) {
		Set<Object> shouldHitTheseTearDowns = Stream.of(values).collect(Collectors.toSet());

		List<Object> missedTearDowns = shouldHitTheseTearDowns.stream()
			.filter(v -> !tearDowns.containsKey(v))
			.collect(Collectors.toList());

		List<Object> unknownTearDowns = tearDowns.keySet().stream()
			.filter(v -> !shouldHitTheseTearDowns.contains(v))
			.collect(Collectors.toList());

		assertThat(missedTearDowns.isEmpty()).describedAs("missed tearDowns: " + missedTearDowns).isTrue();
		assertThat(unknownTearDowns.isEmpty()).describedAs("unknown tearDowns: " + unknownTearDowns).isTrue();
	}

	public void assertTearDownsOrder(Object... values) {
		assertTearDowns(values);

		List<Object> valuesAsList = Stream.of(values).collect(Collectors.toList());
		ArrayList<Object> collectedAsList = new ArrayList<>(tearDowns.keySet());

		assertThat(collectedAsList).describedAs("order of tearDowns").isEqualTo(valuesAsList);
	}
}
