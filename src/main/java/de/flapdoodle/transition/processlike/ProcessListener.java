package de.flapdoodle.transition.processlike;

import java.util.Optional;

import de.flapdoodle.transition.NamedType;
import de.flapdoodle.transition.routes.Route;

public interface ProcessListener {
	<T> void onStateChange(Object oldState, NamedType<T> type, T newState);
	<T> void onStateChangeFailedWithRetry(Route<?> route, Optional<NamedType<T>> type, T state);
}
