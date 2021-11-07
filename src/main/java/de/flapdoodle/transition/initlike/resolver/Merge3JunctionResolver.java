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
import de.flapdoodle.transition.initlike.transitions.Merge3Transition;
import de.flapdoodle.transition.routes.HasDestination;
import de.flapdoodle.transition.routes.Route.Transition;
import de.flapdoodle.transition.routes.Merge3Junction;

class Merge3JunctionResolver implements TransitionResolver {

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public <T> Optional<Function<StateOfNamedType, State<T>>> resolve(HasDestination<T> route, Transition<T> transition) {
		if (route instanceof Merge3Junction && transition instanceof Merge3Transition) {
			return Optional.of(resolveMerge3Junction((Merge3Junction) route, (Merge3Transition) transition));
		}
		return Optional.empty();
	}

	private <A, B, C, T> Function<StateOfNamedType, State<T>> resolveMerge3Junction(Merge3Junction<A, B, C, T> route,
			Merge3Transition<A, B, C, T> transition) {
		return resolver -> transition.apply(resolver.of(route.left()), resolver.of(route.middle()), resolver.of(route.right()));
	}

}