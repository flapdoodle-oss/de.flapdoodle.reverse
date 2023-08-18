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

import de.flapdoodle.checks.Preconditions;

import java.util.Set;

public interface StateLookup {
	<D> D of(StateID<D> type);

	default StateLookup limitedTo(Set<StateID<?>> allowedStates) {
		return limitedTo(allowedStates, this);
	}

	static StateLookup limitedTo(Set<StateID<?>> allowedStates, StateLookup delegate) {
		return new StateLookup() {
			@Override public <D> D of(StateID<D> type) {
				Preconditions.checkArgument(allowedStates.contains(type), "stateID not allowed: %s", type);
				return delegate.of(type);
			}
		};
	}
}