package de.flapdoodle.transition.initlike.transitions;

public interface TriFunction<T, U, V, R> {
	R apply(T t, U u, V v);
}
