package de.flapdoodle.transition.init.edges;

import de.flapdoodle.transition.StateID;
import de.flapdoodle.transition.init.Edge;
import de.flapdoodle.transition.initlike.State;
import de.flapdoodle.transition.initlike.resolver.StateOfNamedType;
import org.immutables.value.Value;

import java.util.function.Function;
import java.util.function.Supplier;

@Value.Immutable
public interface Start<D> extends Edge<D> {
		StateID<D> destination();

		Supplier<State<D>> action();

		@Override
		@Value.Auxiliary
		default Function<StateOfNamedType, State<D>> actionHandler() {
				return lookup -> action().get();
		}

		static <D> Start<D> with(StateID<D> dest, Supplier<State<D>> action) {
				return ImmutableStart.<D>builder()
						.destination(dest)
						.action(action)
						.build();
		}
}
