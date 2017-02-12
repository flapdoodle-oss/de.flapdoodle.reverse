package de.flapdoodle.transition.initlike.transitions;

import java.util.function.Function;

import de.flapdoodle.transition.State;
import de.flapdoodle.transition.routes.Route;

public interface BridgeTransition<S, D> extends Function<S, State<D>>, Route.Transition<D> {

}