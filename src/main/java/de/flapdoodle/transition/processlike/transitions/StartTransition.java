package de.flapdoodle.transition.processlike.transitions;

import java.util.function.Supplier;

import de.flapdoodle.transition.routes.Route;

public interface StartTransition<D> extends Supplier<D>, Route.Transition<D> {

}