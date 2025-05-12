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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TearDownTest {

	@Test
	void fistExceptionWithSecondAsSuppressed() {
		List<String> calls = new ArrayList<>();

		TearDown<String> first = it -> {
			calls.add("first");
			throw new RuntimeException("first exception");
		};
		TearDown<String> second = it -> {
			calls.add("second");
			throw new RuntimeException("second exception");
		};

		TearDown<String> testee = first.andThen(second);

		assertThatThrownBy(() -> testee.onTearDown("irrelevant"))
			.isInstanceOf(RuntimeException.class)
			.hasMessageContaining("first exception")
			.hasSuppressedException(new RuntimeException("second exception"));

		assertThat(calls).containsExactly("first", "second");
	}

	@Test
	void firstExceptionIfSecondDoesNotFail() {
		List<String> calls = new ArrayList<>();

		TearDown<String> first = it -> {
			calls.add("first");
			throw new RuntimeException("first exception");
		};
		TearDown<String> second = it -> {
			// do nothing;
			calls.add("second");
		};

		TearDown<String> testee = first.andThen(second);

		assertThatThrownBy(() -> testee.onTearDown("irrelevant"))
			.isInstanceOf(RuntimeException.class)
			.hasMessageContaining("first exception")
			.hasNoSuppressedExceptions();
		assertThat(calls).containsExactly("first", "second");
	}

	@Test
	void noExceptionIfNothingDoesNotFail() {
		List<String> calls = new ArrayList<>();

		TearDown<String> first = it -> {
			calls.add("first");
		};
		TearDown<String> second = it -> {
			calls.add("second");
		};

		TearDown<String> testee = first.andThen(second);

		testee.onTearDown("irrelevant");
		assertThat(calls).containsExactly("first", "second");
	}

	@Test
	void secondExceptionIfFirstDoesNotFail() {
		List<String> calls = new ArrayList<>();

		TearDown<String> first = it -> {
			calls.add("first");
		};
		TearDown<String> second = it -> {
			calls.add("second");
			throw new RuntimeException("second exception");
		};

		TearDown<String> testee = first.andThen(second);

		assertThatThrownBy(() -> testee.onTearDown("irrelevant"))
			.isInstanceOf(RuntimeException.class)
			.hasMessageContaining("second exception")
			.hasNoSuppressedExceptions();
		assertThat(calls).containsExactly("first", "second");
	}

	@Test
	void callEachTearDownOnAggregatedList() {
		List<String> calls = new ArrayList<>();

		TearDown<String> first = it -> {
			calls.add("first");
			throw new RuntimeException("first exception");
		};
		TearDown<String> second = it -> {
			calls.add("second");
			throw new RuntimeException("second exception");
		};
		TearDown<String> third = it -> {
			calls.add("third");
			throw new RuntimeException("third exception");
		};

		TearDown<String> testee = TearDown.aggregate(first, second, third).get();

		assertThatThrownBy(() -> testee.onTearDown("irrelevant"))
			.isInstanceOf(RuntimeException.class)
			.hasMessageContaining("first exception")
			.hasSuppressedException(new RuntimeException("second exception"))
			.hasSuppressedException(new RuntimeException("third exception"));

		assertThat(calls).containsExactly("first", "second", "third");
	}
}