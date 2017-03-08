package de.flapdoodle.transition.initlike;

import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

import de.flapdoodle.transition.NamedType;

@Immutable
public interface NamedTypeAndValue<T> {
	@Parameter
	NamedType<T> type();
	@Parameter
	T value();
	
	public static <T> NamedTypeAndValue<T> of(NamedType<T> type, T value) {
		return ImmutableNamedTypeAndValue.of(type, value);
	}
}
