package de.flapdoodle.transition.initlike;

import de.flapdoodle.transition.State;

public interface AutocloseableState<T> extends State<T>, AutoCloseable  {

	@Override
	void close() throws RuntimeException;
}
