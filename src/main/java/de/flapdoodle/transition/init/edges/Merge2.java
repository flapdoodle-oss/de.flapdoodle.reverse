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

import java.util.function.BiFunction;
import java.util.function.Function;

@Value.Immutable
public interface Merge2<L, R, D>  extends Edge<D> {
		StateID<L> left();
		StateID<R> right();
		StateID<D> destination();
		BiFunction<L, R, State<D>> action();

		@Override
		@Value.Auxiliary
		default Function<StateOfNamedType, State<D>> actionHandler() {
				return lookup -> action().apply(lookup.of(left()), lookup.of(right()));
		}

		static <L, R, D> Merge2<L, R, D> with(StateID<L> left, StateID<R> right, StateID<D> dest, BiFunction<L, R, State<D>> action) {
				return ImmutableMerge2.<L, R ,D>builder()
						.left(left)
						.right(right)
						.destination(dest)
						.action(action)
						.build();
		}

}
