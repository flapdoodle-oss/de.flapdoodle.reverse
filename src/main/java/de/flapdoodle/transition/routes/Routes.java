package de.flapdoodle.transition.routes;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import de.flapdoodle.transition.routes.Route.Transition;

public class Routes<R extends Route<?>> {
	
	private final Map<R, Transition<?>> routeMap;

	private Routes(Map<R, Transition<?>> routeMap) {
		this.routeMap = new LinkedHashMap<>(routeMap);
		
	}
	
	public Set<R> all() {
		return Collections.unmodifiableSet(routeMap.keySet());
	}
	
	public <D> Transition<D> transitionOf(Route<D> route) {
		return (Transition<D>) routeMap.get(route);
	}
	
	public Routes<SingleDestination<?>> asWithSingleDestinations() {
		Map<SingleDestination<?>, Transition<?>> filtered = this.routeMap.entrySet().stream()
			.filter(e -> e.getKey() instanceof SingleDestination)
			.collect(Collectors.toMap(e -> (SingleDestination) e.getKey(), e -> e.getValue()));
		
		if (filtered.size()!=this.routeMap.size()) {
			throw new IllegalArgumentException("route contains other things than SingleDestionation instances");
		}
		return new Routes<>(filtered);
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
		
		public <S,A,B> Builder add(PartingWay<S,A,B> route, PartingWay.Transition<S,A,B> transition) {
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
		
		public Routes<Route<?>> build() {
			return new Routes<>(routeMap);
		}
	}
}
