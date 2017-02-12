/**
 * Copyright (C) 2016
 *   Michael Mosmann <michael@mosmann.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.flapdoodle.transition.initlike;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import de.flapdoodle.transition.initlike.transitions.BridgeTransition;
import de.flapdoodle.transition.initlike.transitions.MergeTransition;
import de.flapdoodle.transition.initlike.transitions.StartTransition;
import de.flapdoodle.transition.initlike.transitions.ThreeWayMergingTransition;
import de.flapdoodle.transition.routes.Bridge;
import de.flapdoodle.transition.routes.MergingJunction;
import de.flapdoodle.transition.routes.Route;
import de.flapdoodle.transition.routes.Route.Transition;
import de.flapdoodle.transition.routes.SingleDestination;
import de.flapdoodle.transition.routes.Start;
import de.flapdoodle.transition.routes.ThreeWayMergingJunction;

public class InitRoutes<R extends SingleDestination<?>> {

	private final Map<R, Transition<?>> routeMap;

	private InitRoutes(Map<R, Transition<?>> routeMap) {
		this.routeMap = new LinkedHashMap<>(routeMap);
	}

	public Set<R> all() {
		return Collections.unmodifiableSet(routeMap.keySet());
	}

	public <D> Transition<D> transitionOf(SingleDestination<D> route) {
		return (Transition<D>) routeMap.get(route);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		Map<SingleDestination<?>, Route.Transition<?>> routeMap = new LinkedHashMap<>();

		private Builder() {

		}

		public <D> Builder add(Start<D> route, StartTransition<D> transition) {
			return addRoute(route, transition);
		}

		public <S, D> Builder add(Bridge<S, D> route, BridgeTransition<S, D> transition) {
			return addRoute(route, transition);
		}

		public <L, R, D> Builder add(MergingJunction<L, R, D> route, MergeTransition<L, R, D> transition) {
			return addRoute(route, transition);
		}

		public <L, M, R, D> Builder add(ThreeWayMergingJunction<L, M, R, D> route, ThreeWayMergingTransition<L, M, R, D> transition) {
			return addRoute(route, transition);
		}

		private <D> Builder addRoute(SingleDestination<D> route, Route.Transition<D> transition) {
			Transition<?> old = routeMap.put(route, transition);
			if (old != null) {
				throw new IllegalArgumentException("route " + route + " already set to " + old);
			}
			return this;
		}

		public InitRoutes<SingleDestination<?>> build() {
			return new InitRoutes<>(routeMap);
		}
	}
}
