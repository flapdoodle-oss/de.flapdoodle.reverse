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
package de.flapdoodle.transition.process;

import de.flapdoodle.checks.Preconditions;
import de.flapdoodle.transition.StateID;
import de.flapdoodle.transition.process.edges.Conditional;
import de.flapdoodle.transition.process.edges.End;
import de.flapdoodle.transition.process.edges.Start;
import de.flapdoodle.transition.process.edges.Step;
import de.flapdoodle.types.Either;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ProcessEngine {
		private final Start<?> start;
		private final Map<StateID<?>, HasSource<?>> sourceMap;
		private ProcessEngine(Start<?> start, Map<StateID<?>, HasSource<?>> sourceMap) {
				this.start = start;
				this.sourceMap = sourceMap;
		}

		public Started start() {
			return new Started(startWith(start));
		}

		private static <T> State<T> startWith(Start<T> start) {
				return State.of(start.destination(), start.action().get());
		}

		public class Started {
				private State<?> currentState;

				public Started(State<?> currentState) {
						this.currentState = currentState;
				}

				public State<?> currentState() {
						return currentState;
				}

				public boolean next() {
						HasSource nextStep = sourceMap.get(currentState.type());
						Preconditions.checkNotNull(nextStep,"could not find next step for %s", currentState.type());

						Optional<? extends State<?>> nextState = process(nextStep, currentState);
						if (nextState.isPresent()) {
								currentState = nextState.get();
								return true;
						}
						currentState=null;
						return false;
				}

				private <T> Optional<? extends State<?>> process(HasSource<T> next, State<T> currentState) {
						if (next instanceof End) {
								((End<T>) next).action().accept(currentState.value());
								return Optional.empty();
						}
						if (next instanceof Step) {
								Step<T, ?> nextStep = (Step<T, ?>) next;
								return processStep(nextStep, currentState);
						}
						if (next instanceof Conditional) {
								Conditional<T, ?, ?> nextConditional = (Conditional<T, ?, ?>) next;
								return processConditional(nextConditional, currentState);
						}

						throw new IllegalArgumentException("not supported: "+next);
				}

				private <S, D> Optional<? extends State<D>> processStep(Step<S, D> nextStep, State<S> currentState) {
						return Optional.of(State.of(nextStep.destination(), nextStep.action().apply(currentState.value())));
				}

				private <S, D1, D2> Optional<? extends State<?>> processConditional(Conditional<S, D1, D2> nextConditional, State<S> currentState) {
						Either<D1, D2> nextValue = nextConditional.action().apply(currentState.value());
						return Optional.of(nextValue.isLeft()
								? State.of(nextConditional.firstDestination(), nextValue.left())
								: State.of(nextConditional.secondDestination(), nextValue.right()));
				}
		}

//	private final ProcessRoutes<HasSource<?,?>> routes;
//	private final Start<?> start;
//	private final Map<StateID<?>, HasSource<?, ?>> sourceMap;
//
//	private ProcessEngine(ProcessRoutes<HasSource<?,?>> routes, Start<?> start, Map<StateID<?>, HasSource<?,?>> sourceMap) {
//		this.routes = Preconditions.checkNotNull(routes,"routes is null");
//		this.start = Preconditions.checkNotNull(start,"start is null");
//		this.sourceMap = new LinkedHashMap<>(Preconditions.checkNotNull(sourceMap,"sourceMap is null"));
//	}

//	@SuppressWarnings({ "unchecked", "rawtypes" })
//	public <S,D> void run(ProcessListener listener) {
//		HasSource<S,D> currentRoute = (HasSource<S, D>) start;
//
//		Optional<de.flapdoodle.transition.processlike.State<S>> currentState=Optional.empty();
//		Optional<de.flapdoodle.transition.processlike.State<D>> newState=Optional.empty();
//
//		try {
//
//			do {
//				try {
//					newState = run(currentRoute, currentState.map(s -> s.value()).orElse(null));
//					if (newState.isPresent()) {
//						currentRoute = (HasSource<S, D>) sourceMap.get(newState.get().type());
//						D newStateValue = newState.get().value();
//						listener.onStateChange(currentState, newState.get());
//						currentState = (Optional) newState;
//					}
//				} catch (RetryException rx) {
//					Optional<de.flapdoodle.transition.processlike.State<?>> lastState=(Optional) newState;
//					listener.onStateChangeFailedWithRetry(currentRoute, lastState);
//				}
//			} while (newState.isPresent());
//		} catch (RuntimeException rx) {
//			throw new AbortException("aborted", currentRoute, currentState, rx);
//		}
//	}
//
//	@SuppressWarnings("unchecked")
//	private <S,D> Optional<de.flapdoodle.transition.processlike.State<D>> run(HasSource<S,D> currentRoute, S currentState) {
//		Transition<D> transition = routes.transitionOf(currentRoute);
//		if (transition instanceof StartTransition) {
//			return runStart((Start<D>) currentRoute, (StartTransition<D>) transition, currentState);
//		}
//		if (transition instanceof BridgeTransition) {
//			return runBridge((Bridge<S,D>) currentRoute, (BridgeTransition<S,D>) transition, currentState);
//		}
//		if (transition instanceof EndTransition) {
//			return runEnd((EndTransition<S>) transition, currentState);
//		}
//		if (transition instanceof PartingTransition) {
//			return runPartingResolved((PartingWay<S,D,D>) currentRoute, (PartingTransition<S,D,D>) transition, currentState);
//		}
//
//		throw new IllegalArgumentException(""+currentRoute+": could not run "+transition);
//	}
//
//	private <D> Optional<de.flapdoodle.transition.processlike.State<D>> runStart(Start<D> startRoute, StartTransition<D> start, Object currentState) {
//		Preconditions.checkArgument(currentState==null, "starting, but current state: %s",currentState);
//		return Optional.of(de.flapdoodle.transition.processlike.State.of(startRoute.destination(), start.get()));
//	}
//
//	private <S,D> Optional<de.flapdoodle.transition.processlike.State<D>> runBridge(Bridge<S,D> bridgeRoute, BridgeTransition<S,D> bridge, S currentState) {
//		Preconditions.checkNotNull(currentState, "bridge, but current state is null");
//		return Optional.of(de.flapdoodle.transition.processlike.State.of(bridgeRoute.destination(), bridge.apply(currentState)));
//	}
//
//	private static <S,D> Optional<de.flapdoodle.transition.processlike.State<D>> runEnd(EndTransition<S> end, S currentState) {
//		Preconditions.checkNotNull(currentState, "end, but current state is null");
//		end.accept(currentState);
//		return Optional.empty();
//	}
//
//	private static <S,D> Optional<de.flapdoodle.transition.processlike.State<D>> runPartingResolved(PartingWay<S, D, D> route, PartingTransition<S, D, D> transition, S currentState) {
//		Either<Optional<de.flapdoodle.transition.processlike.State<D>>, Optional<de.flapdoodle.transition.processlike.State<D>>> either = runParting(route, transition, currentState);
//		return either.isLeft() ? either.left() : either.right();
//	}
//
//	private static <S,A,B> Either<Optional<de.flapdoodle.transition.processlike.State<A>>, Optional<de.flapdoodle.transition.processlike.State<B>>> runParting(PartingWay<S, A, B> route, PartingTransition<S, A, B> transition, S currentState) {
//		Preconditions.checkNotNull(currentState, "parting, but current state is null");
//		Either<A, B> either = transition.apply(currentState);
//		return either.isLeft()
//				? Either.left(Optional.of(de.flapdoodle.transition.processlike.State.of(route.oneDestination(), either.left())))
//				: Either.right(Optional.of(State.of(route.otherDestination(), either.right())));
//	}
//
		public static ProcessEngine with(List<Edge> edges) {
				List<Start<?>> starts = edges.stream()
						.filter(edge -> edge instanceof Start)
						.map(edge -> (Start<?>) edge)
						.collect(Collectors.toList());

				Preconditions.checkArgument(starts.size()==1,"only a single starting point is supported: %s", starts);

				Start<?> start = starts.get(0);

				Map<StateID<?>, List<HasSource<?>>> groupBySourceMap = edges.stream()
						.filter(it -> it instanceof HasSource)
						.map(it -> (HasSource<?>) it)
						.collect(Collectors.groupingBy(HasSource::source));

				List<Map.Entry<StateID<?>, List<HasSource<?>>>> sourceCollisions = groupBySourceMap.entrySet().stream()
						.filter(it -> it.getValue().size() > 1)
						.collect(Collectors.toList());

				Preconditions.checkArgument(sourceCollisions.isEmpty(),"source id used more than once: %s", sourceCollisions);

				Map<StateID<?>, HasSource<?>> sourceMap = groupBySourceMap.entrySet().stream()
						.collect(Collectors.toMap(Map.Entry::getKey, it -> it.getValue().get(0)));

				return new ProcessEngine(start, sourceMap);
		}

//	public static ProcessEngine with(ProcessRoutes<HasSource<?,?>> routes) {
//		List<Route<?>> starts = routes.all().stream()
//			.filter(r -> r instanceof Start)
//			.collect(Collectors.toList());
//		Preconditions.checkArgument(starts.size()==1, "more or less than one start found: %s",starts);
//
//		Map<StateID<?>, HasSource<?,?>> sourceMap = routes.all().stream()
//			.filter(r -> !(r instanceof Start))
//			.collect(Collectors.toMap(r -> sourceOf(r), r -> r));
//
//		return new ProcessEngine(routes, (Start<?>) starts.get(0), sourceMap);
//	}
//
//	private static <T> StateID<T> sourceOf(HasSource<T,?> route) {
//		return route.start();
//	}
}
