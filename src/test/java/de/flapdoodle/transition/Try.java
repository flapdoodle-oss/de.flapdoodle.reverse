package de.flapdoodle.transition;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface Try {
	
	public static <T,E extends Exception> T get(ThrowingSupplier<T, E> supplier) {
		return asSupplier(supplier).get();
	}
	
	public static <T,E extends Exception> void accept(ThrowingConsumer<T, E> consumer, T value) {
		asConsumer(consumer).accept(value);
	}
	
	public static <T,E extends Exception> Supplier<T> asSupplier(ThrowingSupplier<T, E> supplier) {
		return () -> {
			try {
				return supplier.get();
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		};
	}
	
	public static <T,E extends Exception> Consumer<T> asConsumer(ThrowingConsumer<T, E> consumer) {
		return t -> {
			try {
				consumer.accept(t);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		};
	}
	
	interface ThrowingSupplier<T,E extends Exception> {
		T get() throws E;
	}
	
	interface ThrowingConsumer<T,E extends Exception> {
		void accept(T t) throws E;
	}
}
