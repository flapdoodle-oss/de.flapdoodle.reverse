package de.flapdoodle.transition.initlike;

import java.util.function.BiFunction;
import java.util.function.Function;

import de.flapdoodle.transition.NamedType;
import de.flapdoodle.transition.initlike.transitions.BridgeTransition;
import de.flapdoodle.transition.initlike.transitions.MergeTransition;
import de.flapdoodle.transition.initlike.transitions.StartTransition;
import de.flapdoodle.transition.initlike.transitions.ThreeWayMergingTransition;
import de.flapdoodle.transition.routes.Bridge;
import de.flapdoodle.transition.routes.MergingJunction;
import de.flapdoodle.transition.routes.SingleDestination;
import de.flapdoodle.transition.routes.Start;
import de.flapdoodle.transition.routes.ThreeWayMergingJunction;

public class FluentInitRoutesBuilder {

	InitRoutes.Builder builder = InitRoutes.builder();

	private FluentInitRoutesBuilder() {

	}

	public <T> StartBuilder<T> start(Class<T> type) {
		return start(NamedType.typeOf(type));
	}

	public <T> StartBuilder<T> start(NamedType<T> type) {
		return new StartBuilder<T>(this, type);
	}

	private <T> FluentInitRoutesBuilder start(NamedType<T> type, StartTransition<T> transition) {
		builder.add(Start.of(type), transition);
		return this;
	}

	public <S, D> BridgeBuilder<S, D> bridge(Class<S> source, Class<D> destination) {
		return bridge(NamedType.typeOf(source), NamedType.typeOf(destination));
	}

	public <S, D> BridgeBuilder<S, D> bridge(NamedType<S> source, NamedType<D> destination) {
		return new BridgeBuilder<>(this, source, destination);
	}

	private <S, D> FluentInitRoutesBuilder bridge(NamedType<S> source, NamedType<D> destination,
			BridgeTransition<S, D> transition) {
		builder.add(Bridge.of(source, destination), transition);
		return this;
	}

	public <L, R, D> MergeBuilder<L, R, D> merge(NamedType<L> left, NamedType<R> right, NamedType<D> destination) {
		return new MergeBuilder<>(this, left, right, destination);
	}

	private <L, R, D> FluentInitRoutesBuilder merge(NamedType<L> left, NamedType<R> right, NamedType<D> destination,
			MergeTransition<L, R, D> transition) {
		builder.add(MergingJunction.of(left, right, destination), transition);
		return this;
	}

	public <L, M, R, D> ThreeWayMergeBuilder<L, M, R, D> merge3(NamedType<L> left, NamedType<M> middle,
			NamedType<R> right, NamedType<D> destination) {
		return new ThreeWayMergeBuilder<>(this, left, middle, right, destination);
	}

	private <L, M, R, D> FluentInitRoutesBuilder threeWayMerge(NamedType<L> left, NamedType<M> middle, NamedType<R> right,
			NamedType<D> destination,
			ThreeWayMergingTransition<L, M, R, D> transition) {
		builder.add(ThreeWayMergingJunction.of(left, middle, right, destination), transition);
		return this;
	}

	public InitRoutes<SingleDestination<?>> build() {
		return builder.build();
	}

	public static FluentInitRoutesBuilder builder() {
		return new FluentInitRoutesBuilder();
	}

	public final static class StartBuilder<T> {

		private final FluentInitRoutesBuilder parent;
		private final NamedType<T> type;

		public StartBuilder(FluentInitRoutesBuilder parent, NamedType<T> type) {
			this.parent = parent;
			this.type = type;
		}

		public FluentInitRoutesBuilder with(StartTransition<T> transition) {
			return parent.start(type, transition);
		}

		public FluentInitRoutesBuilder withValue(T value) {
			return parent.start(type, () -> State.of(value));
		}
	}

	public final static class BridgeBuilder<S, D> {

		private final FluentInitRoutesBuilder parent;
		private final NamedType<S> source;
		private final NamedType<D> destination;

		public BridgeBuilder(FluentInitRoutesBuilder parent, NamedType<S> source, NamedType<D> destination) {
			this.parent = parent;
			this.source = source;
			this.destination = destination;
		}

		public FluentInitRoutesBuilder with(BridgeTransition<S, D> transition) {
			return parent.bridge(source, destination, transition);
		}

		public FluentInitRoutesBuilder withMapping(Function<S, D> transition) {
			return parent.bridge(source, destination, s -> State.of(transition.apply(s)));
		}

	}

	public final static class MergeBuilder<L, R, D> {

		private final FluentInitRoutesBuilder parent;
		private final NamedType<L> left;
		private final NamedType<R> right;
		private final NamedType<D> destination;

		public MergeBuilder(FluentInitRoutesBuilder parent, NamedType<L> left, NamedType<R> right,
				NamedType<D> destination) {
			this.parent = parent;
			this.left = left;
			this.right = right;
			this.destination = destination;
		}

		public FluentInitRoutesBuilder with(MergeTransition<L, R, D> transition) {
			return parent.merge(left, right, destination, transition);
		}

		public FluentInitRoutesBuilder withMapping(BiFunction<L, R, D> transition) {
			return parent.merge(left, right, destination, (l, r) -> State.of(transition.apply(l, r)));
		}
	}

	public final static class ThreeWayMergeBuilder<L, M, R, D> {

		private final FluentInitRoutesBuilder parent;
		private final NamedType<L> left;
		private final NamedType<M> middle;
		private final NamedType<R> right;
		private final NamedType<D> destination;

		public ThreeWayMergeBuilder(FluentInitRoutesBuilder parent, NamedType<L> left, NamedType<M> middle,
				NamedType<R> right,
				NamedType<D> destination) {
			this.parent = parent;
			this.left = left;
			this.middle = middle;
			this.right = right;
			this.destination = destination;
		}

		public FluentInitRoutesBuilder with(ThreeWayMergingTransition<L, M, R, D> transition) {
			return parent.threeWayMerge(left, middle, right, destination, transition);
		}
	}

}
