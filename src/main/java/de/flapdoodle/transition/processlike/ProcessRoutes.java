package de.flapdoodle.transition.processlike;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import de.flapdoodle.transition.processlike.transitions.BridgeTransition;
import de.flapdoodle.transition.processlike.transitions.EndTransition;
import de.flapdoodle.transition.processlike.transitions.PartingTransition;
import de.flapdoodle.transition.processlike.transitions.StartTransition;
import de.flapdoodle.transition.routes.Bridge;
import de.flapdoodle.transition.routes.End;
import de.flapdoodle.transition.routes.PartingWay;
import de.flapdoodle.transition.routes.Route;
import de.flapdoodle.transition.routes.Route.Transition;
import de.flapdoodle.transition.routes.SingleSource;
import de.flapdoodle.transition.routes.Start;

public class ProcessRoutes<R extends SingleSource<?,?>> {

	private final Map<R, Transition<?>> routeMap;

	private ProcessRoutes(Map<R, Transition<?>> routeMap) {
		this.routeMap = new LinkedHashMap<>(routeMap);
	}

	public Set<R> all() {
		return Collections.unmodifiableSet(routeMap.keySet());
	}

	public <D> Transition<D> transitionOf(SingleSource<?,D> route) {
		return (Transition<D>) routeMap.get(route);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		Map<SingleSource<?,?>, Route.Transition<?>> routeMap = new LinkedHashMap<>();

		private Builder() {

		}

		public <D> Builder add(Start<D> route, StartTransition<D> transition) {
			return addRoute(route, transition);
		}

		public <S, D> Builder add(Bridge<S, D> route, BridgeTransition<S, D> transition) {
			return addRoute(route, transition);
		}

		public <S, A, B> Builder add(PartingWay<S, A, B> route, PartingTransition<S, A, B> transition) {
			return addRoute(route, transition);
		}

		public <S> Builder add(End<S> route, EndTransition<S> transition) {
			return addRoute(route, transition);
		}

		private <S,D> Builder addRoute(SingleSource<S,D> route, Route.Transition<D> transition) {
			Transition<?> old = routeMap.put(route, transition);
			if (old != null) {
				throw new IllegalArgumentException("route " + route + " already set to " + old);
			}
			return this;
		}

		public ProcessRoutes<SingleSource<?,?>> build() {
			return new ProcessRoutes<>(routeMap);
		}
	}
}
