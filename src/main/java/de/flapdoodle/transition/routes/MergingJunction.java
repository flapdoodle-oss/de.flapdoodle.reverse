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
