package de.flapdoodle.transition.processlike.exceptions;

public class RetryException extends ProcessException {

	public RetryException(String message, Throwable cause) {
		super(message, cause);
	}

	public RetryException(String message) {
		super(message);
	}

	public RetryException(Throwable cause) {
		super(cause);
	}
	
}
