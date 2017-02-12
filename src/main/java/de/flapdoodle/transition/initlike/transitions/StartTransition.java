package de.flapdoodle.transition.initlike.transitions;

import java.util.function.Supplier;

import de.flapdoodle.transition.State;
import de.flapdoodle.transition.routes.Route;
import de.flapdoodle.transition.routes.Route.Transition;

public interface StartTransition<D> extends Supplier<State<D>>, Route.Transition<D> {

}