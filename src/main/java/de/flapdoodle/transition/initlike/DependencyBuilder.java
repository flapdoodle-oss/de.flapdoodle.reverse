/**
 * Copyright (C) 2016
 *   Michael Mosmann <michael@mosmann.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.flapdoodle.transition.initlike;

import java.util.function.BiFunction;
import java.util.function.Function;

import de.flapdoodle.transition.NamedType;
import de.flapdoodle.transition.initlike.transitions.BridgeTransition;
import de.flapdoodle.transition.initlike.transitions.Merge3Transition;
import de.flapdoodle.transition.initlike.transitions.MergeTransition;
import de.flapdoodle.transition.initlike.transitions.StartTransition;
import de.flapdoodle.transition.routes.Bridge;
import de.flapdoodle.transition.routes.Merge3Junction;
import de.flapdoodle.transition.routes.MergingJunction;
import de.flapdoodle.transition.routes.SingleDestination;
import de.flapdoodle.transition.routes.Start;

public class DependencyBuilder {

	InitRoutes.Builder builder = InitRoutes.builder();

	private DependencyBuilder() {

	}

	public <T> DestinationBuilder<T> state(Class<T> type) {
		return state(NamedType.typeOf(type));
	}

	public <T> DestinationBuilder<T> state(NamedType<T> type) {
		return new DestinationBuilder<>(this, type);
	}

	private <T> DependencyBuilder start(NamedType<T> type, StartTransition<T> transition) {
		builder.add(Start.of(type), transition);
		return this;
	}

	private <T> DependencyBuilder replaceStart(NamedType<T> type, StartTransition<T> transition) {
		builder.replace(Start.of(type), transition);
		return this;
	}

	private <S, D> DependencyBuilder bridge(NamedType<S> source, NamedType<D> destination,
			BridgeTransition<S, D> transition) {
		builder.add(Bridge.of(source, destination), transition);
		return this;
	}

	private <S, D> DependencyBuilder replaceBridge(NamedType<S> source, NamedType<D> destination,
			BridgeTransition<S, D> transition) {
		builder.replace(Bridge.of(source, destination), transition);
		return this;
	}

	private <L, R, D> DependencyBuilder merge(NamedType<L> left, NamedType<R> right, NamedType<D> destination,
			MergeTransition<L, R, D> transition) {
		builder.add(MergingJunction.of(left, right, destination), transition);
		return this;
	}

	private <L, R, D> DependencyBuilder replaceMerge(NamedType<L> left, NamedType<R> right, NamedType<D> destination,
			MergeTransition<L, R, D> transition) {
		builder.replace(MergingJunction.of(left, right, destination), transition);
		return this;
	}

	private <L, M, R, D> DependencyBuilder merge3(NamedType<L> left, NamedType<M> middle, NamedType<R> right,
			NamedType<D> destination,
			Merge3Transition<L, M, R, D> transition) {
		builder.add(Merge3Junction.of(left, middle, right, destination), transition);
		return this;
	}

	private <L, M, R, D> DependencyBuilder replaceMerge3(NamedType<L> left, NamedType<M> middle, NamedType<R> right,
			NamedType<D> destination,
			Merge3Transition<L, M, R, D> transition) {
		builder.replace(Merge3Junction.of(left, middle, right, destination), transition);
		return this;
	}

	public DependencyBuilder addAll(InitRoutes<SingleDestination<?>> routes) {
		builder.addAll(routes);
		return this;
	}

	public InitRoutes<SingleDestination<?>> build() {
		return builder.build();
	}

	public static DependencyBuilder builder() {
		return new DependencyBuilder();
	}




	public static class DestinationBuilder<D> {

		private final DependencyBuilder parent;
		private final NamedType<D> destination;

		public DestinationBuilder(DependencyBuilder parent, NamedType<D> destination) {
			this.parent = parent;
			this.destination = destination;
		}

		public StartBuilder<D> requiresNothing() {
			return new StartBuilder<>(parent, destination);
		}

		public <S> BridgeBuilder<S, D> requires(Class<S> source) {
			return requires(NamedType.typeOf(source));
		}

		public <S> BridgeBuilder<S, D> requires(NamedType<S> source) {
			return new BridgeBuilder<>(parent, source, destination);
		}

		public <L, R> MergeBuilder<L, R, D> requires(Class<L> left, Class<R> right) {
			return requires(NamedType.typeOf(left), NamedType.typeOf(right));
		}

		public <L, R> MergeBuilder<L, R, D> requires(NamedType<L> left, NamedType<R> right) {
			return new MergeBuilder<>(parent, left, right, destination);
		}

		public <L, M, R> Merge3Builder<L, M, R, D> requires(Class<L> left, Class<M> middle, Class<R> right) {
			return requires(NamedType.typeOf(left), NamedType.typeOf(middle), NamedType.typeOf(right));
		}

		public <L, M, R> Merge3Builder<L, M, R, D> requires(NamedType<L> left, NamedType<M> middle, NamedType<R> right) {
			return new Merge3Builder<>(parent, left, middle, right, destination);
		}
	}

	public final static class StartBuilder<T> {

		private final DependencyBuilder parent;
		private final NamedType<T> type;
		private boolean replace = false;

		public StartBuilder(DependencyBuilder parent, NamedType<T> type) {
			this.parent = parent;
			this.type = type;
		}

		public StartBuilder<T> replace() {
			replace = true;
			return this;
		}

		public DependencyBuilder with(StartTransition<T> transition) {
			return replace
					? parent.replaceStart(type, transition)
					: parent.start(type, transition);
		}

		public DependencyBuilder withValue(T value) {
			return with(() -> State.of(value));
		}
	}

	public final static class BridgeBuilder<S, D> {

		private final DependencyBuilder parent;
		private final NamedType<S> source;
		private final NamedType<D> destination;
		private boolean replace = false;

		public BridgeBuilder(DependencyBuilder parent, NamedType<S> source, NamedType<D> destination) {
			this.parent = parent;
			this.source = source;
			this.destination = destination;
		}

		public BridgeBuilder<S, D> replace() {
			replace = true;
			return this;
		}

		public DependencyBuilder with(BridgeTransition<S, D> transition) {
			return replace
					? parent.replaceBridge(source, destination, transition)
					: parent.bridge(source, destination, transition);
		}

		public DependencyBuilder withMapping(Function<S, D> transition) {
			return with(s -> State.of(transition.apply(s)));
		}
	}

	public final static class MergeBuilder<L, R, D> {

		private final DependencyBuilder parent;
		private final NamedType<L> left;
		private final NamedType<R> right;
		private final NamedType<D> destination;
		private boolean replace = false;

		public MergeBuilder(DependencyBuilder parent, NamedType<L> left, NamedType<R> right,
				NamedType<D> destination) {
			this.parent = parent;
			this.left = left;
			this.right = right;
			this.destination = destination;
		}

		public MergeBuilder<L, R, D> replace() {
			replace = true;
			return this;
		}

		public DependencyBuilder with(MergeTransition<L, R, D> transition) {
			return replace
					? parent.replaceMerge(left, right, destination, transition)
					: parent.merge(left, right, destination, transition);
		}

		public DependencyBuilder withMapping(BiFunction<L, R, D> transition) {
			return with((l, r) -> State.of(transition.apply(l, r)));
		}
	}

	public final static class Merge3Builder<L, M, R, D> {

		private final DependencyBuilder parent;
		private final NamedType<L> left;
		private final NamedType<M> middle;
		private final NamedType<R> right;
		private final NamedType<D> destination;
		private boolean replace = false;

		public Merge3Builder(DependencyBuilder parent, NamedType<L> left, NamedType<M> middle,
				NamedType<R> right,
				NamedType<D> destination) {
			this.parent = parent;
			this.left = left;
			this.middle = middle;
			this.right = right;
			this.destination = destination;
		}

		public Merge3Builder<L, M, R, D> replace() {
			replace = true;
			return this;
		}

		public DependencyBuilder with(Merge3Transition<L, M, R, D> transition) {
			return replace
					? parent.replaceMerge3(left, middle, right, destination, transition)
					: parent.merge3(left, middle, right, destination, transition);
		}
	}

}
