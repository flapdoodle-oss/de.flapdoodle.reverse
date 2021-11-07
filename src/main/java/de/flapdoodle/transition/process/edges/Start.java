package de.flapdoodle.transition.process.edges;

import de.flapdoodle.transition.StateID;
import de.flapdoodle.transition.process.Edge;
import org.immutables.value.Value;

import java.util.function.Supplier;

@Value.Immutable
public interface Start<D> extends Edge {
		@Value.Parameter
		StateID<D> destination();

		@Value.Parameter
		Supplier<D> action();

		static <D> Start<D> of(StateID<D> destination, Supplier<D> action) {
				return ImmutableStart.of(destination, action);
		}
}
