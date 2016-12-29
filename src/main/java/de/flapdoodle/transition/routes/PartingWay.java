package de.flapdoodle.transition.routes;

import java.util.Set;
import java.util.function.Function;

import org.immutables.value.Value;

import de.flapdoodle.transition.NamedType;
import de.flapdoodle.transition.State;

@Value.Immutable
public interface PartingWay<S,A,B> extends Route<Either<A,B>> {
	NamedType<S> start();
	NamedType<A> oneDestination();
	NamedType<B> otherDestination();
	
	@Override
	default Set<NamedType<?>> sources() {
		return NamedType.setOf(start());
	}
	
	interface Transition<S,A,B> extends Function<State<S>, Either<State<A>,State<B>>>, Route.Transition<Either<A,B>> {
		
	}
	
	public static <S,A,B> PartingWay<S,A,B> of(NamedType<S> start, NamedType<A> oneDestination, NamedType<B> otherDestination) {
		return ImmutablePartingWay.<S,A,B>builder()
				.start(start)
				.oneDestination(oneDestination)
				.otherDestination(otherDestination)
				.build();
	}

}
