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

import java.util.Optional;
import java.util.function.Function;

import de.flapdoodle.transition.State;
import de.flapdoodle.transition.routes.Route.Transition;
import de.flapdoodle.transition.routes.SingleDestination;
import de.flapdoodle.transition.routes.Start;

class StartResolver implements TransitionResolver {

	@Override
	public <T> Optional<Function<StateResolver, State<T>>> resolve(SingleDestination<T> route,	Transition<T> transition) {
		if (route instanceof Start && transition instanceof Start.Transition) {
			return Optional.of(resolveStart((Start.Transition)transition));
		}
		return Optional.empty();
	}

	private <S,T> Function<StateResolver, State<T>> resolveStart(Start.Transition<T> transition) {
		return resolver -> transition.get();
	}
	
}