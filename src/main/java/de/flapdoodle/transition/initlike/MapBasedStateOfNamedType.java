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

import java.util.LinkedHashMap;
import java.util.Map;

import de.flapdoodle.checks.Preconditions;
import de.flapdoodle.transition.NamedType;
import de.flapdoodle.transition.initlike.resolver.StateOfNamedType;

public class MapBasedStateOfNamedType implements StateOfNamedType {

	private final Map<NamedType<?>, State<?>> stateMap;

	public MapBasedStateOfNamedType(Map<NamedType<?>, State<?>> stateMap) {
		this.stateMap = new LinkedHashMap<>(stateMap);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <D> D of(NamedType<D> type) {
		return ((State<D>) Preconditions.checkNotNull(stateMap.get(type), "could find state for %s", type)).value();
	}

}