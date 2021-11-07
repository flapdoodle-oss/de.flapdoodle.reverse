package de.flapdoodle.transition.process.edges;

import de.flapdoodle.transition.StateID;
import de.flapdoodle.transition.process.Edge;
import de.flapdoodle.transition.process.HasSource;
import org.immutables.value.Value;

import java.util.function.Function;

@Value.Immutable
public interface Step<S, D> extends Edge, HasSource<S> {
		@Override
		@Value.Parameter
		StateID<S> source();

		@Value.Parameter
		StateID<D> destination();

		@Value.Parameter
		Function<S, D> action();

		static <S,D> Step<S,D> of(StateID<S> source, StateID<D> destination, Function<S, D> action) {
				return ImmutableStep.of(source, destination, action);
		}

}
