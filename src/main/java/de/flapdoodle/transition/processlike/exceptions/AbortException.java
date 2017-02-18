package de.flapdoodle.transition.processlike.exceptions;

import java.util.Optional;

import de.flapdoodle.transition.NamedType;
import de.flapdoodle.transition.routes.Route;

public class AbortException extends ProcessException {

	private final Route<?> currentRoute;
	private final Optional<NamedType<?>> type;
	private final Object currentState;

	public AbortException(String message, Route<?> currentRoute, Optional<NamedType<?>> type, Object currentState, RuntimeException cause) {
		super(message, cause);
		this.currentRoute = currentRoute;
		this.type = type;
		this.currentState = currentState;
	}
	
}
