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
package de.flapdoodle.reverse;

import org.immutables.builder.Builder;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable
public abstract class TransitionMapping<D> {
	@Builder.Parameter
	public abstract String label();

	@Builder.Parameter
	public abstract StateMapping<D> destination();

	public abstract List<StateMapping<?>> mappings();

	protected <T> Optional<StateID<T>> findDestinationOf(StateID<T> source) {
		return mappings().stream()
			.filter(it -> it.source().equals(source))
			.map(it -> (StateMapping<T>) it)
			.findFirst()
			.map(StateMapping::destination);
	}

	protected <T> StateID<T> destinationOf(StateID<T> source) {
		return findDestinationOf(source).orElse(source);
	}

	protected <T> Optional<StateID<T>> findSourceOf(StateID<T> destination) {
		return mappings().stream()
			.filter(it -> it.destination().equals(destination))
			.map(it -> (StateMapping<T>) it)
			.findFirst()
			.map(StateMapping::source);
	}

	protected <T> StateID<T> sourceOf(StateID<T> destination) {
		return findSourceOf(destination).orElse(destination);
	}

	public static <D> ImmutableTransitionMapping.Builder<D> builder(String label, StateID<D> destination) {
		return builder(label, StateMapping.of(destination, destination));
	}

	public static <D> ImmutableTransitionMapping.Builder<D> builder(String label, StateMapping<D> mapping) {
		return ImmutableTransitionMapping.builder(label, mapping);
	}
}
