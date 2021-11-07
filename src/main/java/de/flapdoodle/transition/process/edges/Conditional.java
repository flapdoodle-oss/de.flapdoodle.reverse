package de.flapdoodle.transition.process.edges;

import de.flapdoodle.transition.StateID;
import de.flapdoodle.transition.process.Edge;
import de.flapdoodle.transition.process.HasSource;
import de.flapdoodle.types.Either;
import org.immutables.value.Value;

import java.util.function.Function;

@Value.Immutable
public interface Conditional<S,D1,D2> extends Edge, HasSource<S> {
		@Override
		@Value.Parameter
		StateID<S> source();

		@Value.Parameter
		StateID<D1> firstDestination();

		@Value.Parameter
		StateID<D2> secondDestination();

		@Value.Parameter
		Function<S, Either<D1, D2>> action();

		static <S,D1, D2> Conditional<S,D1,D2> of(StateID<S> source, StateID<D1> first, StateID<D2> second, Function<S, Either<D1,D2>> action) {
				return ImmutableConditional.of(source,first,second,action);
		}
}
