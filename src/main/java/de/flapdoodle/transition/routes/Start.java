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
package de.flapdoodle.transition.routes;

import java.util.Set;

import org.immutables.value.Value;

import de.flapdoodle.transition.StateID;

@Value.Immutable
public interface Start<D> extends SingleSource<Void, D>,SingleDestination<D> {
	@Override
	default StateID<Void> start() {
		return StateID.typeOf(Void.class);
	}
	
	@Override
	default Set<StateID<?>> sources() {
		return StateID.setOf();
	}

	public static <D> Start<D> of(StateID<D> destination) {
		return ImmutableStart.builder(destination).build();
	}
}
