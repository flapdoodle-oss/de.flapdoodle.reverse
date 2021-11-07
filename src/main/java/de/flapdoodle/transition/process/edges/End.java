package de.flapdoodle.transition.process.edges;

import de.flapdoodle.transition.StateID;
import de.flapdoodle.transition.process.Edge;
import de.flapdoodle.transition.process.HasSource;
import org.immutables.value.Value;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Value.Immutable
public interface End<S> extends Edge, HasSource<S> {
		@Override
		@Value.Parameter
		StateID<S> source();

		@Value.Parameter
		Consumer<S> action();

		static <D> End<D> of(StateID<D> source, Consumer<D> action) {
				return ImmutableEnd.of(source, action);
		}

}
