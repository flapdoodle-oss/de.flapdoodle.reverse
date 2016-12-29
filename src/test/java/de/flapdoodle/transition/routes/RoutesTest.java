package de.flapdoodle.transition.routes;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.flapdoodle.transition.NamedType;
import de.flapdoodle.transition.State;

public class RoutesTest {

	@Test
	public void buildRoutes() {
		Routes routes = Routes.builder()
			.add(Start.of(NamedType.of(String.class)), () -> State.of("12",RoutesTest::tearDown))
			.add(Bridge.of(NamedType.of(String.class), NamedType.of(Integer.class)), a -> a.map(Integer::valueOf, RoutesTest::tearDown))
			.build();
		
		assertEquals(2,routes.all().size());
	}
	
	public static <T> void tearDown(T value) {
		System.out.println("tear down '"+value+"'");
	}

}
