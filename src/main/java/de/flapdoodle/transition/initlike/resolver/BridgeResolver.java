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

import de.flapdoodle.transition.init.State;
import de.flapdoodle.transition.initlike.transitions.BridgeTransition;
import de.flapdoodle.transition.routes.Bridge;
import de.flapdoodle.transition.routes.Route.Transition;
import de.flapdoodle.transition.routes.HasDestination;

class BridgeResolver implements TransitionResolver {

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T> Optional<Function<StateOfNamedType, State<T>>> resolve(HasDestination<T> route, Transition<T> transition) {
		if (route instanceof Bridge && transition instanceof BridgeTransition) {
			return Optional.of(resolveBridge((Bridge) route, (BridgeTransition) transition));
		}
		return Optional.empty();
	}

	private <S, T> Function<StateOfNamedType, State<T>> resolveBridge(Bridge<S, T> route, BridgeTransition<S, T> transition) {
		return resolver -> transition.apply(resolver.of(route.start()));
	}

}