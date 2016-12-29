package de.flapdoodle.transition.routes;

import java.util.Set;
import java.util.function.Supplier;

import org.immutables.value.Value;

import de.flapdoodle.transition.NamedType;
import de.flapdoodle.transition.State;

@Value.Immutable
public interface Start<D> extends SingleDestination<D> {

	@Override
	default Set<NamedType<?>> sources() {
		return NamedType.setOf();
	}

	interface Transition<D> extends Supplier<State<D>>, Route.Transition<D> {
		
	}

	public static <D> Start<D> of(NamedType<D> destination) {
		return ImmutableStart.builder(destination).build();
	}
}
