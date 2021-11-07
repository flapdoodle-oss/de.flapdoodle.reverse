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
package de.flapdoodle.transition.processlike.edges;

import de.flapdoodle.transition.StateID;
import de.flapdoodle.transition.processlike.Edge;
import de.flapdoodle.transition.processlike.HasSource;
import org.immutables.value.Value;

import java.util.function.Consumer;

@Value.Immutable
public interface End<S> extends Edge, HasSource<S> {
		@Override
		@Value.Parameter
		StateID<S> source();

		@Value.Parameter
		Consumer<S> action();

		static <D> End<D> of(StateID<D> source, Consumer<D> action) {
				return ImmutableEnd.of(source, action);
		}

}