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
package de.flapdoodle.transition.initlike;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

import de.flapdoodle.transition.NamedType;

@Immutable
public interface NamedTypeAndState<T> {
	@Parameter
	NamedType<T> type();
	@Parameter
	State<T> state();
	
	@Auxiliary
	default NamedTypeAndValue<T> asTypeAndValue() {
		return NamedTypeAndValue.of(type(), state().value());
	}
	
	public static <T> NamedTypeAndState<T> of(NamedType<T> type, State<T> state) {
		return ImmutableNamedTypeAndState.of(type, state);
	}
}
