package de.flapdoodle.transition.processlike.exceptions;

public class AbortException extends ProcessException {

	public AbortException(String message, Throwable cause) {
		super(message, cause);
	}

	public AbortException(String message) {
		super(message);
	}

	public AbortException(Throwable cause) {
		super(cause);
	}
	
}
