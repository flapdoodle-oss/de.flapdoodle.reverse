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
package de.flapdoodle.transition.initlike.resolver;

import java.util.Optional;
import java.util.function.Function;

import de.flapdoodle.transition.initlike.State;
import de.flapdoodle.transition.initlike.transitions.ThreeWayMergingTransition;
import de.flapdoodle.transition.routes.Route.Transition;
import de.flapdoodle.transition.routes.SingleDestination;
import de.flapdoodle.transition.routes.ThreeWayMergingJunction;

class ThreeWayMergingJunctionResolver implements TransitionResolver {

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public <T> Optional<Function<StateOfNamedType, State<T>>> resolve(SingleDestination<T> route, Transition<T> transition) {
		if (route instanceof ThreeWayMergingJunction && transition instanceof ThreeWayMergingTransition) {
			return Optional.of(resolveThreeWayMergingJunction((ThreeWayMergingJunction) route, (ThreeWayMergingTransition) transition));
		}
		return Optional.empty();
	}

	private <A, B, C, T> Function<StateOfNamedType, State<T>> resolveThreeWayMergingJunction(ThreeWayMergingJunction<A, B, C, T> route,
			ThreeWayMergingTransition<A, B, C, T> transition) {
		return resolver -> transition.apply(resolver.of(route.left()), resolver.of(route.middle()), resolver.of(route.right()));
	}

}