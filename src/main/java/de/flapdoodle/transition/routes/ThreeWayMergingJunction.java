package de.flapdoodle.transition.routes;

import java.util.Set;

import org.immutables.value.Value;

import de.flapdoodle.transition.NamedType;
import de.flapdoodle.transition.State;

@Value.Immutable
public interface ThreeWayMergingJunction<L,M,R,D> extends Route<D> {
	NamedType<L> left();
	NamedType<M> middle();
	NamedType<R> right();
	
	@Override
	default Set<NamedType<?>> sources() {
		return NamedType.setOf(left(),middle(),right());
	}

	
	interface Transition<L,M,R,D> extends Route.Transition<D> {
		State<D> apply(State<L> t, State<L> m, State<R> r);
	}

	public static <L,M,R,D> ThreeWayMergingJunction<L,M,R,D> of(NamedType<L> left, NamedType<M> middle, NamedType<R> right, NamedType<D> destination) {
		return ImmutableThreeWayMergingJunction.<L,M,R,D>builder(destination)
				.left(left)
				.middle(middle)
				.right(right)
				.build();
	}

}
