package de.flapdoodle.transition.routes;

import org.immutables.builder.Builder.Parameter;

import de.flapdoodle.transition.NamedType;

public interface SingleDestination<D> extends Route<D> {
	
	@Parameter
	NamedType<D> destination();

}
