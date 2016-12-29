package de.flapdoodle.transition.routes;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import de.flapdoodle.transition.routes.Route.Transition;

public class Routes {
	
	private final Map<Route<?>, Transition<?>> routeMap;

	private Routes(Map<Route<?>, Transition<?>> routeMap) {
		this.routeMap = new LinkedHashMap<>(routeMap);
		
	}
	
	public Set<Route<?>> all() {
		return Collections.unmodifiableSet(routeMap.keySet());
	}
	
	public <D> Transition<D> transitionOf(Route<D> route) {
		return (Transition<D>) routeMap.get(route);
	}
	
	public static Builder builder() {
		return new Builder();
	}
	
	public static class Builder {
		Map<Route<?>, Route.Transition<?>> routeMap=new LinkedHashMap<>();
		
		private Builder() {
			
		}
		
		public <D> Builder add(Start<D> route, Start.Transition<D> transition) {
			return addRoute(route,transition);
		}
		
		public <S,D> Builder add(Bridge<S,D> route, Bridge.Transition<S,D> transition) {
			return addRoute(route,transition);
		}
		
		public <S,D> Builder add(Funnel<S,D> route, Funnel.Transition<S,D> transition) {
			return addRoute(route,transition);
		}
		
		public <L,R,D> Builder add(MergingJunction<L,R,D> route, MergingJunction.Transition<L,R,D> transition) {
			return addRoute(route,transition);
		}
		
		public <L,M,R,D> Builder add(ThreeWayMergingJunction<L,M,R,D> route, ThreeWayMergingJunction.Transition<L,M,R,D> transition) {
			return addRoute(route,transition);
		}
		
		private <D> Builder addRoute(Route<D> route, Route.Transition<D> transition) {
			Transition<?> old = routeMap.put(route, transition);
			if (old!=null) {
				throw new IllegalArgumentException("route "+route+" already set to "+old);
			}
			return this;
		}
		
		public Routes build() {
			return new Routes(routeMap);
		}
	}
}
