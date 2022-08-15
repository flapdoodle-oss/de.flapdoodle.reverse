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
package de.flapdoodle.reverse;

import org.immutables.value.Value.Auxiliary;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@FunctionalInterface
public interface TearDown<T> {
	void onTearDown(T current);

	@Auxiliary
	default TearDown<T> andThen(TearDown<T> next) {
		return t -> {
			this.onTearDown(t);
			next.onTearDown(t);
		};
	}

	public static <T> Optional<TearDown<T>> aggregate(TearDown<T>... tearDowns) {
		if (tearDowns.length > 0) {
			List<TearDown<T>> asList = Arrays.asList(tearDowns);
			return Optional.of(current -> asList.forEach(t -> t.onTearDown(current)));
		}
		return Optional.empty();
	}

	static <T> TearDown<T> wrap(Consumer<T> wrap) {
		return wrap::accept;
	}
}
