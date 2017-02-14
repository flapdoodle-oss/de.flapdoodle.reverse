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
package de.flapdoodle.transition.types;

import java.util.Optional;

import org.immutables.value.Value;
import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Check;

@Value.Immutable
public abstract class Either<L, R> {

	protected abstract Optional<L> optLeft();

	protected abstract Optional<R> optRight();

	@Auxiliary
	public boolean isLeft() {
		return optLeft().isPresent();
	}
	
	@Auxiliary
	public L left() {
		return optLeft().get();
	}
	
	@Auxiliary
	public R right() {
		return optRight().get();
	}
	
	@Check
	protected void check() {
		if (optLeft().isPresent() && optRight().isPresent()) {
			throw new IllegalArgumentException("is both: " + optLeft() + "," + optRight());
		}
		if (!optLeft().isPresent() && !optRight().isPresent()) {
			throw new IllegalArgumentException("is nothing");
		}
	}

	public static <L, R> Either<L, R> left(L left) {
		return ImmutableEither.<L, R> builder().optLeft(left).build();
	}

	public static <L, R> Either<L, R> right(R right) {
		return ImmutableEither.<L, R> builder().optRight(right).build();
	}
}
