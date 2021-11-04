package de.flapdoodle.transition.init.edges;

import de.flapdoodle.transition.StateID;
import de.flapdoodle.transition.init.Edge;
import de.flapdoodle.transition.initlike.State;
import de.flapdoodle.transition.initlike.resolver.StateOfNamedType;
import de.flapdoodle.transition.initlike.transitions.TriFunction;
import org.immutables.value.Value;

import java.util.function.Function;

@Value.Immutable
public interface Merge3<L, M, R, D> extends Edge<D> {
		StateID<L> left();
		StateID<M> middle();
		StateID<R> right();
		StateID<D> destination();
		TriFunction<L, M, R, State<D>> action();

		@Override
		@Value.Auxiliary
		default Function<StateOfNamedType, State<D>> actionHandler() {
				return lookup -> action().apply(lookup.of(left()), lookup.of(middle()), lookup.of(right()));
		}

		static <L, M, R, D> Merge3<L, M, R, D> with(StateID<L> left, StateID<M> middle, StateID<R> right, StateID<D> dest, TriFunction<L, M, R, State<D>> action) {
				return ImmutableMerge3.<L, M, R ,D>builder()
						.left(left)
						.middle(middle)
						.right(right)
						.destination(dest)
						.action(action)
						.build();
		}
}
