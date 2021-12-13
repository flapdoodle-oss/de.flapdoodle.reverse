package de.flapdoodle.reverse;

import org.immutables.builder.Builder;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable
public abstract class TransitionMapping<D> {
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

	public static <D> ImmutableTransitionMapping.Builder<D> builder(StateID<D> destination) {
		return builder(StateMapping.of(destination, destination));
	}

	public static <D> ImmutableTransitionMapping.Builder<D> builder(StateMapping<D> mapping) {
		return ImmutableTransitionMapping.builder(mapping);
	}
}
