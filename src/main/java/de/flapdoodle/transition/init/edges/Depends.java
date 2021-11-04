package de.flapdoodle.transition.init.edges;

import de.flapdoodle.transition.StateID;
import de.flapdoodle.transition.init.Edge;
import de.flapdoodle.transition.initlike.State;
import de.flapdoodle.transition.initlike.resolver.StateOfNamedType;
import org.immutables.value.Value;

import java.util.function.Function;

@Value.Immutable
public interface Depends<S,D> extends Edge<D> {
		StateID<S> source();
		StateID<D> destination();
		Function<S, State<D>> action();

		@Override
		@Value.Auxiliary
		default Function<StateOfNamedType, State<D>> actionHandler() {
				return lookup -> action().apply(lookup.of(source()));
		}

		static <S, D> Depends<S,D> with(StateID<S> source, StateID<D> dest, Function<S, State<D>> action) {
				return ImmutableDepends.<S,D>builder()
						.source(source)
						.destination(dest)
						.action(action)
						.build();
		}
}
