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

import de.flapdoodle.transition.StateID;
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

	InitRoutes.RawBuilder builder = InitRoutes.rawBuilder();

	private DependencyBuilder() {

	}

	public <T> StartBuilder<T> state(Class<T> id) {
		return state(StateID.of(id));
	}

	public <T> StartBuilder<T> state(StateID<T> id) {
		return new StartBuilder<>(this, id);
	}

	public <T> GivenBridgeBuilder<T> given(Class<T> source) {
		return given(StateID.of(source));
	}

	public <T> GivenBridgeBuilder<T> given(StateID<T> source) {
		return new GivenBridgeBuilder<>(this, source);
	}

	public <L, R> GivenMergeBuilder<L, R> given(Class<L> left, Class<R> right) {
		return given(StateID.of(left), StateID.of(right));
	}

	public <L, R> GivenMergeBuilder<L, R> given(StateID<L> left, StateID<R> right) {
		return new GivenMergeBuilder<>(this, left, right);
	}

	public <L, M, R> GivenMerge3Builder<L, M, R> given(Class<L> left, Class<M> middle, Class<R> right) {
		return given(StateID.of(left), StateID.of(middle), StateID.of(right));
	}

	public <L, M, R> GivenMerge3Builder<L, M, R> given(StateID<L> left, StateID<M> middle, StateID<R> right) {
		return new GivenMerge3Builder<>(this, left, middle, right);
	}



	private <T> DependencyBuilder start(StateID<T> type, StartTransition<T> transition) {
		builder.add(Start.of(type), transition);
		return this;
	}

	private <T> DependencyBuilder replaceStart(StateID<T> type, StartTransition<T> transition) {
		builder.replace(Start.of(type), transition);
		return this;
	}



	private <S, D> DependencyBuilder bridge(StateID<S> source, StateID<D> destination,
			BridgeTransition<S, D> transition) {
		builder.add(Bridge.of(source, destination), transition);
		return this;
	}

	private <S, D> DependencyBuilder replaceBridge(StateID<S> source, StateID<D> destination,
			BridgeTransition<S, D> transition) {
		builder.replace(Bridge.of(source, destination), transition);
		return this;
	}



	private <L, R, D> DependencyBuilder merge(StateID<L> left, StateID<R> right, StateID<D> destination,
			MergeTransition<L, R, D> transition) {
		builder.add(MergingJunction.of(left, right, destination), transition);
		return this;
	}

	private <L, R, D> DependencyBuilder replaceMerge(StateID<L> left, StateID<R> right, StateID<D> destination,
			MergeTransition<L, R, D> transition) {
		builder.replace(MergingJunction.of(left, right, destination), transition);
		return this;
	}



	private <L, M, R, D> DependencyBuilder merge3(StateID<L> left, StateID<M> middle, StateID<R> right,
			StateID<D> destination,
			Merge3Transition<L, M, R, D> transition) {
		builder.add(Merge3Junction.of(left, middle, right, destination), transition);
		return this;
	}

	private <L, M, R, D> DependencyBuilder replaceMerge3(StateID<L> left, StateID<M> middle, StateID<R> right,
			StateID<D> destination,
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



	public final static class NothingGivenBuilder {

		private final DependencyBuilder parent;

		public NothingGivenBuilder(DependencyBuilder parent) {
			this.parent = parent;
		}

		public <T> StartBuilder<T> state(Class<T> destination) {
			return state(StateID.of(destination));
		}

		public <T> StartBuilder<T> state(StateID<T> destination) {
			return new StartBuilder<>(parent, destination);
		}
	}

	public final static class StartBuilder<T> {

		private final DependencyBuilder parent;
		private final StateID<T> type;
		private boolean replace = false;

		public StartBuilder(DependencyBuilder parent, StateID<T> type) {
			this.parent = parent;
			this.type = type;
		}

		public StartBuilder<T> replace() {
			replace = true;
			return this;
		}

		public DependencyBuilder isReachedBy(StartTransition<T> transition) {
			return replace
					? parent.replaceStart(type, transition)
					: parent.start(type, transition);
		}

		public DependencyBuilder isInitializedWith(T value) {
			return isReachedBy(() -> State.of(value));
		}
	}


	public final static class GivenBridgeBuilder<S> {

		private final DependencyBuilder parent;
		private final StateID<S> source;

		public GivenBridgeBuilder(DependencyBuilder parent, StateID<S> source) {
			this.parent = parent;
			this.source = source;
		}

		public <D> BridgeBuilder<S, D> state(Class<D> destination) {
			return state(StateID.of(destination));
		}

		public <D> BridgeBuilder<S, D> state(StateID<D> destination) {
			return new BridgeBuilder<>(parent, source, destination);
		}
	}

	public final static class BridgeBuilder<S, D> {

		private final DependencyBuilder parent;
		private final StateID<S> source;
		private final StateID<D> destination;
		private boolean replace = false;

		public BridgeBuilder(DependencyBuilder parent, StateID<S> source, StateID<D> destination) {
			this.parent = parent;
			this.source = source;
			this.destination = destination;
		}

		public BridgeBuilder<S, D> replace() {
			replace = true;
			return this;
		}

		public DependencyBuilder isReachedBy(BridgeTransition<S, D> transition) {
			return replace
					? parent.replaceBridge(source, destination, transition)
					: parent.bridge(source, destination, transition);
		}

		public DependencyBuilder isReachedByMapping(Function<S, D> transition) {
			return isReachedBy(s -> State.of(transition.apply(s)));
		}
	}


	public final static class GivenMergeBuilder<L, R> {

		private final DependencyBuilder parent;
		private final StateID<L> left;
		private final StateID<R> right;

		public GivenMergeBuilder(DependencyBuilder parent, StateID<L> left, StateID<R> right) {
			this.parent = parent;
			this.left = left;
			this.right = right;
		}

		public <D> MergeBuilder<L, R, D> state(Class<D> destination) {
			return state(StateID.of(destination));
		}

		public <D> MergeBuilder<L, R, D> state(StateID<D> destination) {
			return new MergeBuilder<>(parent, left, right, destination);
		}
	}

	public final static class MergeBuilder<L, R, D> {

		private final DependencyBuilder parent;
		private final StateID<L> left;
		private final StateID<R> right;
		private final StateID<D> destination;
		private boolean replace = false;

		public MergeBuilder(DependencyBuilder parent, StateID<L> left, StateID<R> right,
				StateID<D> destination) {
			this.parent = parent;
			this.left = left;
			this.right = right;
			this.destination = destination;
		}

		public MergeBuilder<L, R, D> replace() {
			replace = true;
			return this;
		}

		public DependencyBuilder isReachedBy(MergeTransition<L, R, D> transition) {
			return replace
					? parent.replaceMerge(left, right, destination, transition)
					: parent.merge(left, right, destination, transition);
		}

		public DependencyBuilder isReachedByMapping(BiFunction<L, R, D> transition) {
			return isReachedBy((l, r) -> State.of(transition.apply(l, r)));
		}
	}


	public final static class GivenMerge3Builder<L, M, R> {

		private final DependencyBuilder parent;
		private final StateID<L> left;
		private final StateID<M> middle;
		private final StateID<R> right;

		public GivenMerge3Builder(DependencyBuilder parent, StateID<L> left, StateID<M> middle, StateID<R> right) {
			this.parent = parent;
			this.left = left;
			this.middle = middle;
			this.right = right;
		}

		public <D> Merge3Builder<L, M, R, D> state(Class<D> destination) {
			return state(StateID.of(destination));
		}

		public <D> Merge3Builder<L, M, R, D> state(StateID<D> destination) {
			return new Merge3Builder<>(parent, left, middle, right, destination);
		}
	}


	public final static class Merge3Builder<L, M, R, D> {

		private final DependencyBuilder parent;
		private final StateID<L> left;
		private final StateID<M> middle;
		private final StateID<R> right;
		private final StateID<D> destination;
		private boolean replace = false;

		public Merge3Builder(DependencyBuilder parent, StateID<L> left, StateID<M> middle,
				StateID<R> right,
				StateID<D> destination) {
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

		public DependencyBuilder isReachedBy(Merge3Transition<L, M, R, D> transition) {
			return replace
					? parent.replaceMerge3(left, middle, right, destination, transition)
					: parent.merge3(left, middle, right, destination, transition);
		}
	}

}
