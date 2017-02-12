package de.flapdoodle.transition.initlike.transitions;

import de.flapdoodle.transition.State;
import de.flapdoodle.transition.routes.Route;

public interface ThreeWayMergingTransition<L, M, R, D> extends Route.Transition<D> {
	State<D> apply(L t, M m, R r);
}