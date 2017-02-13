package de.flapdoodle.transition.processlike;

import de.flapdoodle.transition.NamedType;
import de.flapdoodle.transition.routes.Route;

public interface ProcessListener {
	<T> void onStateChange(Object oldState, NamedType<T> type, T newState);
	<T> void onStateChangeFailedWithRetry(Route<?> route, NamedType<T> type, T state);
}
