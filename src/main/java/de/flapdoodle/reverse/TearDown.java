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

import org.immutables.value.Value.Auxiliary;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

@FunctionalInterface
public interface TearDown<T> {
	void onTearDown(T current);

	@Auxiliary
	default TearDown<T> andThen(TearDown<T> next) {
		return t -> {
			RuntimeException first = null;
			RuntimeException second = null;
			try {
				this.onTearDown(t);
			} catch (RuntimeException ex) {
				first = ex;
			}

			try {
				next.onTearDown(t);
			} catch (RuntimeException ex) {
				second = ex;
			}

			if (first != null && second != null) {
				first.addSuppressed(second);
				throw first;
			}

			if (first != null || second != null) {
				throw first != null ? first : second;
			}
		};
	}

	@SafeVarargs
	static <T> Optional<TearDown<T>> aggregate(TearDown<T>... tearDowns) {
		if (tearDowns.length > 0) {
			TearDown<T> first = tearDowns[0];
			if (tearDowns.length == 1) {
				return Optional.of(first);
			} else {
				TearDown<T> reduced = Stream.of(tearDowns)
					.skip(1)
					.reduce(first, TearDown::andThen);
				return Optional.of(reduced);
			}
		}
		return Optional.empty();
	}

	static <T> TearDown<T> wrap(Consumer<T> wrap) {
		return wrap::accept;
	}
}
