package de.flapdoodle.reverse;

import java.util.function.Consumer;
import java.util.function.Function;

public interface ThrowingConsumer<T, E extends Exception> {
	void accept(T value) throws E;

	static <T, E extends Exception> Consumer<T> wrap(
		ThrowingConsumer<T, E> delegate
	) {
		return wrap(delegate, RuntimeException::new);
	}

	static <T, E extends Exception> Consumer<T> wrap(
		ThrowingConsumer<T, E> delegate,
		Function<Exception, RuntimeException> mapException
	) {
		return (value) -> {
			try {
				delegate.accept(value);
			}
			catch (Exception e) {
				throw mapException.apply(e);
			}
		};
	}
}
