package de.flapdoodle.transition.initlike;

import de.flapdoodle.transition.NamedType;
import de.flapdoodle.transition.State;

interface StateResolver {
	<D> State<D> resolve(NamedType<D> type);
}