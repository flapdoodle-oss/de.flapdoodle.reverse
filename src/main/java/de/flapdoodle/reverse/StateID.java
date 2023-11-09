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

import de.flapdoodle.reflection.TypeInfo;
import de.flapdoodle.reverse.types.TypeNames;
import org.immutables.value.Value;
import org.immutables.value.Value.Parameter;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Value.Immutable
public interface StateID<T> {
	@Parameter
	String name();

	@Parameter
	TypeInfo<T> type();

	static <T> StateID<T> of(String name, Class<T> type) {
		return ImmutableStateID.of(name, TypeInfo.of(type));
	}

	static <T> StateID<T> of(String name, TypeInfo<T> type) {
		return ImmutableStateID.of(name, type);
	}

	static <T> StateID<T> of(Class<T> type) {
		return of("", type);
	}

	static <T> StateID<T> of(TypeInfo<T> type) {
		return of("", type);
	}

	static Set<StateID<?>> setOf(StateID<?>... namedTypes) {
		return Collections.unmodifiableSet(Stream.of(namedTypes).collect(Collectors.toSet()));
	}

	static String asLabel(StateID<?> t) {
		return (t.name().isEmpty() ? "<empty>" : t.name()) + ":" + TypeNames.typeName(t.type());
	}
}
