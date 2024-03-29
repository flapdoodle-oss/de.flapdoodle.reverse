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
package de.flapdoodle.reverse.types;

import java.util.Objects;
import java.util.function.Function;

public interface TriFunction<T, U, V, R> {
	R apply(T t, U u, V v);

	default <R2> TriFunction<T, U, V, R2> andThen(Function<? super R, ? extends R2> after) {
		Objects.requireNonNull(after);
		return (t, u, v) -> after.apply(this.apply(t, u, v));
	}

}
