package de.flapdoodle.transition.routes;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.flapdoodle.transition.NamedType;
import de.flapdoodle.transition.State;

public class RoutesTest {

	@Test
	public void buildRoutes() {
		Routes<Route<?>> routes = Routes.builder()
			.add(Start.of(typeOf(String.class)), () -> State.of("12",RoutesTest::tearDown))
			.add(Bridge.of(typeOf(String.class), typeOf(Integer.class)), a -> a.map(Integer::valueOf, RoutesTest::tearDown))
			.build();
		
		assertEquals(2,routes.all().size());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void routesContainingPartingWayCanNotBeHandledAsSingleDestination() {
		Routes<SingleDestination<?>> routes = Routes.builder()
			.add(Start.of(typeOf(String.class)), () -> State.of("12",RoutesTest::tearDown))
			.add(PartingWay.of(typeOf(String.class), typeOf("left", String.class), typeOf("right", String.class)), s -> Either.left(s))
			.add(Bridge.of(typeOf(String.class), typeOf(Integer.class)), a -> a.map(Integer::valueOf, RoutesTest::tearDown))
			.build()
			.asWithSingleDestinations();
	}
	
	private static <T> NamedType<T> typeOf(Class<T> type) {
		return NamedType.of(type);
	}
	
	private static <T> NamedType<T> typeOf(String name, Class<T> type) {
		return NamedType.of(name, type);
	}
	
	private static <T> void tearDown(T value) {
		System.out.println("tear down '"+value+"'");
	}

}
