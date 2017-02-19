package de.flapdoodle.transition.processlike;

import java.util.Optional;

import de.flapdoodle.transition.routes.Route;

public interface ProcessOnStateChangeFailedWithRetry {
	void onStateChangeFailedWithRetry(Route<?> route, Optional<? extends State<?>> currentState);
}
