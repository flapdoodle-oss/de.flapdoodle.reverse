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
package de.flapdoodle.transition;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.immutables.builder.Builder.Parameter;
import org.immutables.value.Value;
import org.immutables.value.Value.Auxiliary;

@Value.Immutable
public interface State<T> {
	@Parameter
	T current();
	
	List<TearDown<T>> onTearDown();
	
	@Auxiliary
	default <D> State<D> map(Function<T, D> map, TearDown<D> ... tearDowns) {
		return builder(map.apply(current()))
				.addOnTearDown(tearDowns)
				.build();
	}
	
	public static <T> ImmutableState.Builder<T> builder(T current) {
		return ImmutableState.builder(current);
	}
	
	public static <T> State<T> of(T current, TearDown<T> ... tearDowns) {
		return builder(current)
				.addOnTearDown(tearDowns)
				.build();
	}
	
	public static <A,B,D> State<D> merge(State<A> a, State<B> b, BiFunction<A, B, D> merge, TearDown<D> ... tearDowns) {
		return builder(merge.apply(a.current(), b.current()))
				.addOnTearDown(tearDowns)
				.build();
	}
}
