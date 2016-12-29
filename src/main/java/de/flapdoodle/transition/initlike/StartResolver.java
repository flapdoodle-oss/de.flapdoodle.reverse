package de.flapdoodle.transition.initlike;

import java.util.Optional;
import java.util.function.Function;

import de.flapdoodle.transition.State;
import de.flapdoodle.transition.routes.Route.Transition;
import de.flapdoodle.transition.routes.SingleDestination;
import de.flapdoodle.transition.routes.Start;

class StartResolver implements TransitionResolver {

	@Override
	public <T> Optional<Function<StateResolver, State<T>>> resolve(SingleDestination<T> route,	Transition<T> transition) {
		if (route instanceof Start && transition instanceof Start.Transition) {
			return Optional.of(resolveStart((Start.Transition)transition));
		}
		return Optional.empty();
	}

	private <S,T> Function<StateResolver, State<T>> resolveStart(Start.Transition<T> transition) {
		return resolver -> transition.get();
	}
	
}