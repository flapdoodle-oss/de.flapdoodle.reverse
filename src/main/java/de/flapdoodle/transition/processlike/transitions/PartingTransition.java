package de.flapdoodle.transition.processlike.transitions;

import java.util.function.Function;

import de.flapdoodle.transition.routes.Route;
import de.flapdoodle.transition.types.Either;

public interface PartingTransition<S,A,B> extends Function<S, Either<A,B>>, Route.Transition<Either<A,B>> {
	
}