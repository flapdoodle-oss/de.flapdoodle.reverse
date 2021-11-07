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
package de.flapdoodle.transition.initlike.edges;

import de.flapdoodle.transition.StateID;
import de.flapdoodle.transition.initlike.Edge;
import de.flapdoodle.transition.initlike.State;
import de.flapdoodle.transition.types.TriFunction;
import org.immutables.value.Value;

@Value.Immutable
public interface Merge3<L, M, R, D> extends Edge<D> {
		StateID<L> left();
		StateID<M> middle();
		StateID<R> right();
		StateID<D> destination();
		TriFunction<L, M, R, State<D>> action();

		static <L, M, R, D> Merge3<L, M, R, D> of(StateID<L> left, StateID<M> middle, StateID<R> right, StateID<D> dest, TriFunction<L, M, R, State<D>> action) {
				return ImmutableMerge3.<L, M, R ,D>builder()
						.left(left)
						.middle(middle)
						.right(right)
						.destination(dest)
						.action(action)
						.build();
		}
}
