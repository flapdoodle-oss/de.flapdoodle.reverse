package de.flapdoodle.transition.routes;

import java.util.Set;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Lazy;

import de.flapdoodle.transition.NamedType;

public interface Route<D> {
	@Auxiliary
	@Lazy
	Set<NamedType<?>> sources();
	
	interface Transition<D> {
		
	}
}
