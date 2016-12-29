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
package de.flapdoodle.transition.routes;

import java.util.Set;
import java.util.function.Function;

import org.immutables.value.Value;

import de.flapdoodle.transition.NamedType;
import de.flapdoodle.transition.State;

@Value.Immutable
public interface Bridge<S,D> extends SingleDestination<D> {
	NamedType<S> start();
	
	@Override
	default Set<NamedType<?>> sources() {
		return NamedType.setOf(start());
	}
	
	interface Transition<S,D> extends Function<State<S>, State<D>>, Route.Transition<D> {
		
	}
	
	public static <S,D> Bridge<S,D> of(NamedType<S> start, NamedType<D> destination) {
		return ImmutableBridge.<S,D>builder(destination)
				.start(start)
				.build();
	}

}
