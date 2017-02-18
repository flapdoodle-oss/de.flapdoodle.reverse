package de.flapdoodle.transition.routes;

import de.flapdoodle.transition.NamedType;

public interface SingleSource<S,D> extends Route<D> {
	NamedType<S> start();
}
