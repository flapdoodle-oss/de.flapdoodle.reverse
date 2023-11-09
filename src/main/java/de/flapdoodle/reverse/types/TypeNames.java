/*
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
package de.flapdoodle.reverse.types;

import de.flapdoodle.reflection.ClassTypeInfo;
import de.flapdoodle.reflection.ListTypeInfo;
import de.flapdoodle.reflection.TypeInfo;
import de.flapdoodle.types.Pair;

public class TypeNames {

	private TypeNames() {
		// no instance
	}

	public static String typeName(TypeInfo<?> type) {
		if (type instanceof ClassTypeInfo) {
			Class<?> t = ((ClassTypeInfo<?>) type).type();
			return typeName(t);
		}
		if (type instanceof ListTypeInfo) {
			return "List<"+typeName(((ListTypeInfo<?>) type).elements())+">";
		}
		if (type instanceof Pair.PairTypeInfo) {
			Pair.PairTypeInfo<?, ?> pair = (Pair.PairTypeInfo<?, ?>) type;
			return "Pair<"+typeName(pair.first())+","+typeName(pair.second())+">";
		}
		if (type instanceof HasTypeName) {
			return ((HasTypeName) type).typeName();
		}
		return type.toString();
	}

	@Deprecated
	public static String typeName(Class<?> type) {
		String ret = type.getSimpleName();
		if (ret.isEmpty()) {
			ret = type.getTypeName();
		}
		return ret;
	}
}
