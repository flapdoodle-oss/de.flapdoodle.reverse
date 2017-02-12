package de.flapdoodle.transition.processlike;

import org.junit.Test;

import de.flapdoodle.transition.NamedType;
import de.flapdoodle.transition.routes.Bridge;
import de.flapdoodle.transition.routes.End;
import de.flapdoodle.transition.routes.Route;
import de.flapdoodle.transition.routes.Start;

public class ProcessEngineLikeTest {

	@Test
	public void simpleSample() {
		ProcessRoutes<Route<?>> routes = ProcessRoutes.builder()
				.add(Start.of(typeOf(String.class)), () -> "12")
				.add(Bridge.of(typeOf(String.class), typeOf(Integer.class)), a -> Integer.valueOf(a))
				.add(End.of(typeOf(Integer.class)), i -> {})
				.build();

	}

	private static <T> NamedType<T> typeOf(Class<T> type) {
		return NamedType.typeOf(type);
	}

	private static <T> NamedType<T> typeOf(String name, Class<T> type) {
		return NamedType.typeOf(name, type);
	}

}
