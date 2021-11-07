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
package de.flapdoodle.transition.process.edges;

import de.flapdoodle.transition.StateID;
import de.flapdoodle.transition.process.Edge;
import org.immutables.value.Value;

import java.util.function.Supplier;

@Value.Immutable
public interface Start<D> extends Edge {
		@Value.Parameter
		StateID<D> destination();

		@Value.Parameter
		Supplier<D> action();

		static <D> Start<D> of(StateID<D> destination, Supplier<D> action) {
				return ImmutableStart.of(destination, action);
		}
}
