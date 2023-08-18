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

import org.immutables.value.Value;

@Value.Immutable
public abstract class StateMapping<T> {
	@Value.Parameter
	public abstract StateID<T> source();

	@Value.Parameter
	public abstract StateID<T> destination();

	public boolean isDirect() {
		return source().equals(destination());
	}

	public static <T> StateMapping<T> of(StateID<T> source, StateID<T> destination) {
		return ImmutableStateMapping.of(source, destination);
	}
}
