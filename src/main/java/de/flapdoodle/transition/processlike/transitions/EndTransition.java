package de.flapdoodle.transition.processlike.transitions;

import java.util.function.Consumer;

import de.flapdoodle.transition.routes.Route;

public interface EndTransition<S> extends Consumer<S>, Route.Transition<Void> {

}