package de.flapdoodle.transition.initlike;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.flapdoodle.transition.routes.SingleDestination;

public class DependencyBuilderTest {

	@Test
	public void buildRoutes() {
		InitRoutes<SingleDestination<?>> routes = InitRoutes.dependencyBuilder()
				.given().state(String.class)
				.isReachedBy(() -> State.of("12", DependencyBuilderTest::tearDown))
				.given().state(String.class)
				.replace().isReachedBy(() -> State.of("13", DependencyBuilderTest::tearDown))
				.given(String.class).state(Integer.class)
				.isReachedBy(a -> State.of(Integer.valueOf(a), DependencyBuilderTest::tearDown))
				.build();

		assertEquals(2, routes.all().size());
	}

	private static <T> void tearDown(T value) {
		System.out.println("tear down '" + value + "'");
	}

}
