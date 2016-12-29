package de.flapdoodle.transition.routes;

import java.util.Optional;

import org.immutables.value.Value;
import org.immutables.value.Value.Check;

@Value.Immutable
public abstract class Either<L,R> {
	
	protected abstract Optional<L> left();
	protected abstract Optional<R> right();
	
	
	@Check
	protected void check() {
		if (left().isPresent() && right().isPresent()) {
			throw new IllegalArgumentException("is both: "+left()+","+right());
		}
		if (!left().isPresent() && !right().isPresent()) {
			throw new IllegalArgumentException("is nothing");
		}
	}
	
	public static <L,R> Either<L,R> left(L left) {
		return ImmutableEither.<L,R>builder().left(left).build();
	}
	
	public static <L,R> Either<L,R> right(R right) {
		return ImmutableEither.<L,R>builder().right(right).build();
	}
}
