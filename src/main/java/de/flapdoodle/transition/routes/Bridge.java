package de.flapdoodle.transition.routes;

import java.util.Set;
import java.util.function.Function;

import org.immutables.value.Value;

import de.flapdoodle.transition.NamedType;
import de.flapdoodle.transition.State;

@Value.Immutable
public interface Bridge<S,D> extends Route<D> {
	NamedType<S> start();
	
	@Override
	default Set<NamedType<?>> sources() {
		return NamedType.setOf(start());
	}
	
	interface Transition<S,D> extends Function<State<S>, State<D>>, Route.Transition<D> {
		
	}
	
	public static <S,D> Bridge<S,D> of(NamedType<S> start, NamedType<D> destination) {
		return ImmutableBridge.<S,D>builder(destination)
				.start(start)
				.build();
	}

}
