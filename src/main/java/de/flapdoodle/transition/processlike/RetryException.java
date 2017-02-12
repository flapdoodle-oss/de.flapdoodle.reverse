package de.flapdoodle.transition.processlike;

public class RetryException extends RuntimeException {

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
