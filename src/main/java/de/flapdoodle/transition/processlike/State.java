package de.flapdoodle.transition.processlike;

import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

import de.flapdoodle.transition.NamedType;

@Immutable
public interface State<T> {
	@Parameter
	NamedType<T> type();
	@Parameter
	T value();
	
	public static <T> State<T> of(NamedType<T> type, T value) {
		return ImmutableState.of(type, value);
	}
}
