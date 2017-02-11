package de.flapdoodle.transition.initlike.transitions;

import java.util.function.BiFunction;

import de.flapdoodle.transition.State;
import de.flapdoodle.transition.routes.Route;

public interface MergeTransition<L,R,D> extends BiFunction<L, R, State<D>>, Route.Transition<D> {
	
}