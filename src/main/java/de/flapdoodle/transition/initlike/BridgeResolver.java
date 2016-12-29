package de.flapdoodle.transition.initlike;

import java.util.Optional;
import java.util.function.Function;

import de.flapdoodle.transition.State;
import de.flapdoodle.transition.routes.Bridge;
import de.flapdoodle.transition.routes.Route.Transition;
import de.flapdoodle.transition.routes.SingleDestination;

class BridgeResolver implements TransitionResolver {

	@Override
	public <T> Optional<Function<StateResolver, State<T>>> resolve(SingleDestination<T> route,	Transition<T> transition) {
		if (route instanceof Bridge && transition instanceof Bridge.Transition) {
			return Optional.of(resolveBridge((Bridge) route, (Bridge.Transition)transition));
		}
		return Optional.empty();
	}

	private <S,T> Function<StateResolver, State<T>> resolveBridge(Bridge<S,T> route, Bridge.Transition<S,T> transition) {
		return resolver -> transition.apply(resolver.resolve(route.start()));
	}
	
}