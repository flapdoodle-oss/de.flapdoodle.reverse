package de.flapdoodle.reverse;

import org.immutables.value.Value;

@Value.Immutable
public abstract class StateMapping<T> {
	@Value.Parameter
	public abstract StateID<T> source();

	@Value.Parameter
	public abstract StateID<T> destination();

	public boolean isDirect() {
		return source().equals(destination());
	}

	public static <T> StateMapping<T> of(StateID<T> source, StateID<T> destination) {
		return ImmutableStateMapping.of(source, destination);
	}
}
