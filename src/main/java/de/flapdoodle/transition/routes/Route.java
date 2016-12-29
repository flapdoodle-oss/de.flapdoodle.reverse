package de.flapdoodle.transition.routes;

import java.util.Set;

import org.immutables.builder.Builder.Parameter;
import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Lazy;

import de.flapdoodle.transition.NamedType;

public interface Route<D> {
	@Parameter
	NamedType<D> destination();
	
	@Auxiliary
	@Lazy
	Set<NamedType<?>> sources();
	
	interface Transition<D> {
		
	}
}
