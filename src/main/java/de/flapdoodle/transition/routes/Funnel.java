package de.flapdoodle.transition.routes;

import java.util.Collection;
import java.util.Set;
import java.util.function.Function;

import org.immutables.value.Value;

import de.flapdoodle.transition.NamedType;
import de.flapdoodle.transition.State;

@Value.Immutable
public interface Funnel<S,D> extends SingleDestination<D> {
	NamedType<S> starts();
	
	@Override
	default Set<NamedType<?>> sources() {
		return NamedType.setOf(starts());
	}
	
	interface Transition<S,D> extends Function<Collection<State<S>>, State<D>>, Route.Transition<D> {
		
	}
	
	public static <S,D> Funnel<S,D> of(NamedType<S> start, NamedType<D> destination) {
		return ImmutableFunnel.<S,D>builder(destination)
				.starts(start)
				.build();
	}

}
