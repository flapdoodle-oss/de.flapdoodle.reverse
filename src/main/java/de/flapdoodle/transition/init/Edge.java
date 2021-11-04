package de.flapdoodle.transition.init;

import de.flapdoodle.transition.StateID;
import de.flapdoodle.transition.initlike.State;
import de.flapdoodle.transition.initlike.resolver.StateOfNamedType;
import org.immutables.value.Value;

import java.util.function.Function;

public interface Edge<D> {
		StateID<D> destination();

		Function<StateOfNamedType, State<D>> actionHandler();
}
