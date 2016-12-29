package de.flapdoodle.transition.initlike;

import java.util.Optional;
import java.util.function.Function;

import de.flapdoodle.transition.State;
import de.flapdoodle.transition.routes.SingleDestination;
import de.flapdoodle.transition.routes.Route.Transition;

interface TransitionResolver {
	<T> Optional<Function<StateResolver, State<T>>> resolve(SingleDestination<T> route, Transition<T> transition);
}