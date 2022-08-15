package de.flapdoodle.reverse;

import java.util.function.Function;

public interface ThrowingFunction<T, R, E extends Exception> {
	R apply(T t) throws E;

	static <T, R, E extends Exception> Function<T,R> wrap(
		ThrowingFunction<T,R, E> delegate
	) {
		return wrap(delegate, RuntimeException::new);
	}

	static <T, R, E extends Exception> Function<T,R> wrap(
		ThrowingFunction<T,R, E> delegate,
		Function<Exception, RuntimeException> onException
	) {
		return t -> {
			try {
				return delegate.apply(t);
			}
			catch (Exception e) {
				throw onException.apply(e);
			}
		};
	}
}
