package de.flapdoodle.transition.initlike;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

import de.flapdoodle.transition.NamedType;

@Immutable
public interface NamedTypeAndState<T> {
	@Parameter
	NamedType<T> type();
	@Parameter
	State<T> state();
	
	@Auxiliary
	default NamedTypeAndValue<T> asTypeAndValue() {
		return NamedTypeAndValue.of(type(), state().value());
	}
	
	public static <T> NamedTypeAndState<T> of(NamedType<T> type, State<T> state) {
		return ImmutableNamedTypeAndState.of(type, state);
	}
}
