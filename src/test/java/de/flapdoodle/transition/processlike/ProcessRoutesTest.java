package de.flapdoodle.transition.processlike;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.flapdoodle.transition.NamedType;
import de.flapdoodle.transition.routes.Bridge;
import de.flapdoodle.transition.routes.End;
import de.flapdoodle.transition.routes.Route;
import de.flapdoodle.transition.routes.Start;

public class ProcessRoutesTest {

	@Test
	public void buildRoutes() {
		ProcessRoutes<Route<?>> routes = ProcessRoutes.builder()
				.add(Start.of(typeOf(String.class)), () -> "12")
				.add(Bridge.of(typeOf(String.class), typeOf(Integer.class)), a -> Integer.valueOf(a))
				.add(End.of(typeOf(Integer.class)), i -> {})
				.build();

		assertEquals(3, routes.all().size());
	}

	private static <T> NamedType<T> typeOf(Class<T> type) {
		return NamedType.typeOf(type);
	}

	private static <T> NamedType<T> typeOf(String name, Class<T> type) {
		return NamedType.typeOf(name, type);
	}
}
