package de.flapdoodle.transition.initlike;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TearDownException extends RuntimeException {

	private final List<RuntimeException> exceptions;

	public TearDownException(String message, List<RuntimeException> exceptions) {
		super(message);
		this.exceptions = new ArrayList<>(exceptions);
	}
	
	public TearDownException(String message, RuntimeException cause) {
		super(message,cause);
		this.exceptions = new ArrayList<>();
		this.exceptions.add(cause);
	}
	
	public List<RuntimeException> getExceptions() {
		return Collections.unmodifiableList(exceptions);
	}

}
