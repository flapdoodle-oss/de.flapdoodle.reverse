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
package de.flapdoodle.transition.init.edges;

import de.flapdoodle.transition.StateID;
import de.flapdoodle.transition.init.Edge;
import de.flapdoodle.transition.initlike.State;
import de.flapdoodle.transition.initlike.resolver.StateOfNamedType;
import org.immutables.value.Value;

import java.util.function.Function;

@Value.Immutable
public interface Depends<S,D> extends Edge<D> {
		StateID<S> source();
		StateID<D> destination();
		Function<S, State<D>> action();

		@Override
		@Value.Auxiliary
		default Function<StateOfNamedType, State<D>> actionHandler() {
				return lookup -> action().apply(lookup.of(source()));
		}

		static <S, D> Depends<S,D> with(StateID<S> source, StateID<D> dest, Function<S, State<D>> action) {
				return ImmutableDepends.<S,D>builder()
						.source(source)
						.destination(dest)
						.action(action)
						.build();
		}
}
