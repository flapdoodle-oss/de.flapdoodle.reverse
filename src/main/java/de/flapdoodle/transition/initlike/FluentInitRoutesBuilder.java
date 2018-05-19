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

	private <T> FluentInitRoutesBuilder replaceStart(NamedType<T> type, StartTransition<T> transition) {
		builder.replace(Start.of(type), transition);
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

	private <S, D> FluentInitRoutesBuilder replaceBridge(NamedType<S> source, NamedType<D> destination,
			BridgeTransition<S, D> transition) {
		builder.replace(Bridge.of(source, destination), transition);
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

	private <L, R, D> FluentInitRoutesBuilder replaceMerge(NamedType<L> left, NamedType<R> right, NamedType<D> destination,
			MergeTransition<L, R, D> transition) {
		builder.replace(MergingJunction.of(left, right, destination), transition);
		return this;
	}



	public <L, M, R, D> Merge3Builder<L, M, R, D> merge3(NamedType<L> left, NamedType<M> middle,
			NamedType<R> right, NamedType<D> destination) {
		return new Merge3Builder<>(this, left, middle, right, destination);
	}

	private <L, M, R, D> FluentInitRoutesBuilder merge3(NamedType<L> left, NamedType<M> middle, NamedType<R> right,
			NamedType<D> destination,
			Merge3Transition<L, M, R, D> transition) {
		builder.add(Merge3Junction.of(left, middle, right, destination), transition);
		return this;
	}

	private <L, M, R, D> FluentInitRoutesBuilder replaceMerge3(NamedType<L> left, NamedType<M> middle, NamedType<R> right,
			NamedType<D> destination,
			Merge3Transition<L, M, R, D> transition) {
		builder.replace(Merge3Junction.of(left, middle, right, destination), transition);
		return this;
	}



	public FluentInitRoutesBuilder addAll(InitRoutes<SingleDestination<?>> routes) {
		builder.addAll(routes);
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
		private boolean replace = false;

		public StartBuilder(FluentInitRoutesBuilder parent, NamedType<T> type) {
			this.parent = parent;
			this.type = type;
		}

		public StartBuilder<T> replace() {
			replace=true;
			return this;
		}

		public FluentInitRoutesBuilder with(StartTransition<T> transition) {
			return replace
					? parent.replaceStart(type, transition)
					: parent.start(type, transition);
		}

		public FluentInitRoutesBuilder withValue(T value) {
			return with(() -> State.of(value));
		}
	}



	public final static class BridgeBuilder<S, D> {

		private final FluentInitRoutesBuilder parent;
		private final NamedType<S> source;
		private final NamedType<D> destination;
		private boolean replace=false;

		public BridgeBuilder(FluentInitRoutesBuilder parent, NamedType<S> source, NamedType<D> destination) {
			this.parent = parent;
			this.source = source;
			this.destination = destination;
		}

		public BridgeBuilder<S, D> replace() {
			replace=true;
			return this;
		}

		public FluentInitRoutesBuilder with(BridgeTransition<S, D> transition) {
			return replace
					? parent.replaceBridge(source, destination, transition)
					: parent.bridge(source, destination, transition);
		}

		public FluentInitRoutesBuilder withMapping(Function<S, D> transition) {
			return with(s -> State.of(transition.apply(s)));
		}
	}



	public final static class MergeBuilder<L, R, D> {

		private final FluentInitRoutesBuilder parent;
		private final NamedType<L> left;
		private final NamedType<R> right;
		private final NamedType<D> destination;
		private boolean replace=false;

		public MergeBuilder(FluentInitRoutesBuilder parent, NamedType<L> left, NamedType<R> right,
				NamedType<D> destination) {
			this.parent = parent;
			this.left = left;
			this.right = right;
			this.destination = destination;
		}

		public MergeBuilder<L, R, D> replace() {
			replace=true;
			return this;
		}

		public FluentInitRoutesBuilder with(MergeTransition<L, R, D> transition) {
			return replace
					? parent.replaceMerge(left, right, destination, transition)
					: parent.merge(left, right, destination, transition);
		}

		public FluentInitRoutesBuilder withMapping(BiFunction<L, R, D> transition) {
			return with((l, r) -> State.of(transition.apply(l, r)));
		}
	}



	public final static class Merge3Builder<L, M, R, D> {

		private final FluentInitRoutesBuilder parent;
		private final NamedType<L> left;
		private final NamedType<M> middle;
		private final NamedType<R> right;
		private final NamedType<D> destination;
		private boolean replace=false;

		public Merge3Builder(FluentInitRoutesBuilder parent, NamedType<L> left, NamedType<M> middle,
				NamedType<R> right,
				NamedType<D> destination) {
			this.parent = parent;
			this.left = left;
			this.middle = middle;
			this.right = right;
			this.destination = destination;
		}

		public Merge3Builder<L, M, R, D> replace() {
			replace=true;
			return this;
		}

		public FluentInitRoutesBuilder with(Merge3Transition<L, M, R, D> transition) {
			return replace
					? parent.replaceMerge3(left, middle, right, destination, transition)
					: parent.merge3(left, middle, right, destination, transition);
		}
	}

}