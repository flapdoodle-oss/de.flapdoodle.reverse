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
import java.util.function.BiFunction;

import org.immutables.value.Value;

import de.flapdoodle.transition.NamedType;
import de.flapdoodle.transition.State;

@Value.Immutable
public interface MergingJunction<L,R,D> extends SingleDestination<D> {
	NamedType<L> left();
	NamedType<R> right();
	
	@Override
	default Set<NamedType<?>> sources() {
		return NamedType.setOf(left(),right());
	}

	interface Transition<L,R,D> extends BiFunction<State<L>, State<R>, State<D>>, Route.Transition<D> {
		
	}

	public static <L,R,D> MergingJunction<L,R,D> of(NamedType<L> left, NamedType<R> right, NamedType<D> destination) {
		return ImmutableMergingJunction.<L,R,D>builder(destination)
				.left(left)
				.right(right)
				.build();
	}

}