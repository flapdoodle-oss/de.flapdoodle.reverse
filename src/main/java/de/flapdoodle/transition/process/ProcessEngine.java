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

import java.util.*;
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
				private boolean finished=false;

				private Started(State<?> currentState) {
						this.currentState = currentState;
				}

				public State<?> currentState() {
						Preconditions.checkArgument(!finished,"process already finished");
						return currentState;
				}

				public boolean next() {
						Preconditions.checkNotNull(currentState,"current state is null");
						Preconditions.checkArgument(!finished,"process already finished");

						HasSource nextStep = sourceMap.get(currentState.type());
						Preconditions.checkNotNull(nextStep,"could not find next step for %s", currentState.type());

						Optional<? extends State<?>> nextState = process(nextStep, currentState);
						if (nextState.isPresent()) {
								currentState = nextState.get();
								return true;
						}
						currentState=null;
						finished=true;
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

		private static Set<StateID<?>> destinations(Edge edge) {
				if (edge instanceof End) return StateID.setOf();
				if (edge instanceof Start) return StateID.setOf(((Start<?>) edge).destination());
				if (edge instanceof Step) return StateID.setOf(((Step<?, ?>) edge).destination());
				if (edge instanceof Conditional) return StateID.setOf(((Conditional<?, ?, ?>) edge).firstDestination(), ((Conditional<?, ?, ?>) edge).secondDestination());
				throw new IllegalArgumentException("not supported: "+edge);
		}

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

				Set<StateID<?>> destinationsWithoutStart = edges.stream()
						.flatMap(it -> destinations(it).stream())
						.filter(dest -> !sourceMap.containsKey(dest))
						.collect(Collectors.toSet());

				Preconditions.checkArgument(destinationsWithoutStart.isEmpty(),"unconnected destinations: %s", destinationsWithoutStart);

				return new ProcessEngine(start, sourceMap);
		}
}
