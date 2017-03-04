/**
 * Copyright (C) 2016
 *   Michael Mosmann <michael@mosmann.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
