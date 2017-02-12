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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;

import de.flapdoodle.transition.State;
import de.flapdoodle.transition.routes.Route.Transition;
import de.flapdoodle.transition.routes.SingleDestination;

public interface TransitionResolver {
	<T> Optional<Function<StateOfNamedType, State<T>>> resolve(SingleDestination<T> route, Transition<T> transition);

	static Collection<TransitionResolver> DEFAULT_RESOLVERS = Collections.unmodifiableList(Arrays.asList(new StartResolver(),
			new BridgeResolver(),
			new MergingJunctionResolver(),
			new ThreeWayMergingJunctionResolver()));

	static Collection<TransitionResolver> defaultResolvers() {
		return DEFAULT_RESOLVERS;
	}

	static <T> Optional<Function<StateOfNamedType, State<T>>> resolverOf(Collection<TransitionResolver> transitionResolvers, SingleDestination<T> route,
			Transition<T> transition) {
		for (TransitionResolver resolver : transitionResolvers) {
			Optional<Function<StateOfNamedType, State<T>>> resolvedTransition = resolver.resolve(route, transition);
			if (resolvedTransition.isPresent()) {
				return resolvedTransition;
			}
		}
		return Optional.empty();
	}
}