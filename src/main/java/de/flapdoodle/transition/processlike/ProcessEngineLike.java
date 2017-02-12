package de.flapdoodle.transition.processlike;

import de.flapdoodle.transition.routes.Route;

public class ProcessEngineLike {

	private final ProcessRoutes<Route<?>> routes;

	private ProcessEngineLike(ProcessRoutes<Route<?>> routes) {
		this.routes = routes;
	}

	public static ProcessEngineLike with(ProcessRoutes<Route<?>> routes) {
		return new ProcessEngineLike(routes);
	}
}
