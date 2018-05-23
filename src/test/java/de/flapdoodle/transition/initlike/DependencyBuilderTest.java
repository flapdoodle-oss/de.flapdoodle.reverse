package de.flapdoodle.transition.initlike;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.flapdoodle.transition.routes.SingleDestination;

public class DependencyBuilderTest {

	@Test
	public void buildRoutes() {
		InitRoutes<SingleDestination<?>> routes = InitRoutes.dependencyBuilder()
				
				.state(String.class).requiresNothing()
				.with(() -> State.of("12", DependencyBuilderTest::tearDown))
				.state(String.class).requiresNothing()
				.replace().with(() -> State.of("13", DependencyBuilderTest::tearDown))
				.state(Integer.class).requires(String.class)
				.with(a -> State.of(Integer.valueOf(a), DependencyBuilderTest::tearDown))
				.build();

		assertEquals(2, routes.all().size());
	}

	private static <T> void tearDown(T value) {
		System.out.println("tear down '" + value + "'");
	}

}
