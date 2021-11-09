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
import org.immutables.value.Value;

import java.util.function.Supplier;

@Value.Immutable
public interface Start<D> extends Edge<D> {
		StateID<D> destination();

		Supplier<State<D>> action();

		static <D> Start<D> of(StateID<D> dest, Supplier<State<D>> action) {
				return ImmutableStart.<D>builder()
						.destination(dest)
						.action(action)
						.build();
		}

		static <D> WithDestination<D> to(StateID<D> dest) {
				return new WithDestination(dest);
		}

		static <D> WithDestination<D> to(Class<D> destType) {
				return to(StateID.of(destType));
		}

		class WithDestination<T> {
				private final StateID<T> state;

				private WithDestination(StateID<T> state) {
						this.state = state;
				}

				public Start<T> initializedWith(T value) {
						return with(() -> State.of(value));
				}

				public Start<T> providedBy(Supplier<T> valueSupplier) {
						return with(() -> State.of(valueSupplier.get()));
				}

				public Start<T> with(Supplier<State<T>> supplier) {
						return Start.of(state, supplier);
				}
		}

}
