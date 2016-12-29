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
import java.util.function.Supplier;

import org.immutables.value.Value;

import de.flapdoodle.transition.NamedType;
import de.flapdoodle.transition.State;

@Value.Immutable
public interface Start<D> extends SingleDestination<D> {

	@Override
	default Set<NamedType<?>> sources() {
		return NamedType.setOf();
	}

	interface Transition<D> extends Supplier<State<D>>, Route.Transition<D> {
		
	}

	public static <D> Start<D> of(NamedType<D> destination) {
		return ImmutableStart.builder(destination).build();
	}
}
