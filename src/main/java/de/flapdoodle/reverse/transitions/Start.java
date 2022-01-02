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
package de.flapdoodle.reverse.transitions;

import de.flapdoodle.reverse.State;
import de.flapdoodle.reverse.StateID;
import de.flapdoodle.reverse.StateLookup;
import de.flapdoodle.reverse.Transition;
import de.flapdoodle.reverse.naming.HasLabel;
import org.immutables.value.Value;

import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;

@Value.Immutable
public abstract class Start<D> implements Transition<D>, HasLabel {
	public abstract StateID<D> destination();

	protected abstract Supplier<State<D>> action();

	@Override
	@Value.Default
	public String transitionLabel() {
		return "Start";
	}

	@Override
	@Value.Lazy
	public Set<StateID<?>> sources() {
		return Collections.emptySet();
	}

	@Override
	@Value.Auxiliary
	public State<D> result(StateLookup lookup) {
		return action().get();
	}

	public static <D> ImmutableStart<D> of(StateID<D> dest, Supplier<State<D>> action) {
		return ImmutableStart.<D>builder()
			.destination(dest)
			.action(action)
			.build();
	}

	public static <D> WithDestination<D> to(StateID<D> dest) {
		return new WithDestination(dest);
	}

	public static <D> WithDestination<D> to(Class<D> destType) {
		return to(StateID.of(destType));
	}

	public static class WithDestination<T> {
		private final StateID<T> state;

		private WithDestination(StateID<T> state) {
			this.state = state;
		}

		public ImmutableStart<T> initializedWith(T value) {
			return with(() -> State.of(value));
		}

		public ImmutableStart<T> providedBy(Supplier<T> valueSupplier) {
			return with(() -> State.of(valueSupplier.get()));
		}

		public ImmutableStart<T> with(Supplier<State<T>> supplier) {
			return Start.of(state, supplier);
		}
	}

}
