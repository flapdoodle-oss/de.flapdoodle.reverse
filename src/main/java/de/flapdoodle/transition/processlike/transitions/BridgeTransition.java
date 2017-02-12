package de.flapdoodle.transition.processlike.transitions;

import java.util.function.Function;

import de.flapdoodle.transition.routes.Route;

public interface BridgeTransition<S, D> extends Function<S, D>, Route.Transition<D> {

}