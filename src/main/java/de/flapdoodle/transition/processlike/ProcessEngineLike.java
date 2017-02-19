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
package de.flapdoodle.transition.processlike;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import de.flapdoodle.transition.NamedType;
import de.flapdoodle.transition.Preconditions;
import de.flapdoodle.transition.processlike.exceptions.AbortException;
import de.flapdoodle.transition.processlike.exceptions.RetryException;
import de.flapdoodle.transition.processlike.transitions.BridgeTransition;
import de.flapdoodle.transition.processlike.transitions.EndTransition;
import de.flapdoodle.transition.processlike.transitions.PartingTransition;
import de.flapdoodle.transition.processlike.transitions.StartTransition;
import de.flapdoodle.transition.routes.Bridge;
import de.flapdoodle.transition.routes.PartingWay;
import de.flapdoodle.transition.routes.Route;
import de.flapdoodle.transition.routes.Route.Transition;
import de.flapdoodle.transition.routes.SingleSource;
import de.flapdoodle.transition.routes.Start;
import de.flapdoodle.transition.types.Either;

public class ProcessEngineLike {

	private final ProcessRoutes<SingleSource<?,?>> routes;
	private final Start<?> start;
	private final Map<NamedType<?>, SingleSource<?, ?>> sourceMap;

	private ProcessEngineLike(ProcessRoutes<SingleSource<?,?>> routes, Start<?> start, Map<NamedType<?>, SingleSource<?,?>> sourceMap) {
		this.routes = Preconditions.checkNotNull(routes,"routes is null");
		this.start = Preconditions.checkNotNull(start,"start is null");
		this.sourceMap = new LinkedHashMap<>(Preconditions.checkNotNull(sourceMap,"sourceMap is null"));
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <S,D> void run(ProcessListener listener) {
		SingleSource<S,D> currentRoute = (SingleSource<S, D>) start;
		
		Optional<State<S>> currentState=Optional.empty();
		Optional<State<D>> newState=Optional.empty();
		
		try {
			
			do {
				try {
					newState = run(currentRoute, currentState.map(s -> s.value()).orElse(null));
					if (newState.isPresent()) {
						currentRoute = (SingleSource<S, D>) sourceMap.get(newState.get().type());
						D newStateValue = newState.get().value();
						listener.onStateChange(currentState, newState.get());
						currentState = (Optional) newState;
					}
				} catch (RetryException rx) {
					Optional<State<?>> lastState=(Optional) newState;
					listener.onStateChangeFailedWithRetry(currentRoute, lastState);
				}
			} while (newState.isPresent());
		} catch (RuntimeException rx) {
			throw new AbortException("aborted", currentRoute, currentState, rx);
		}
	}

	@SuppressWarnings("unchecked")
	private <S,D> Optional<State<D>> run(SingleSource<S,D> currentRoute, S currentState) {
		Transition<D> transition = routes.transitionOf(currentRoute);
		if (transition instanceof StartTransition) {
			return runStart((Start<D>) currentRoute, (StartTransition<D>) transition, currentState);
		}
		if (transition instanceof BridgeTransition) {
			return runBridge((Bridge<S,D>) currentRoute, (BridgeTransition<S,D>) transition, currentState);
		}
		if (transition instanceof EndTransition) {
			return runEnd((EndTransition<S>) transition, currentState);
		}
		if (transition instanceof PartingTransition) {
			return runPartingResolved((PartingWay<S,D,D>) currentRoute, (PartingTransition<S,D,D>) transition, currentState);
		}
		
		throw new IllegalArgumentException(""+currentRoute+": could not run "+transition);
	}

	private <D> Optional<State<D>> runStart(Start<D> startRoute, StartTransition<D> start, Object currentState) {
		Preconditions.checkArgument(currentState==null, "starting, but current state: %s",currentState);
		return Optional.of(State.of(startRoute.destination(), start.get()));
	}

	private <S,D> Optional<State<D>> runBridge(Bridge<S,D> bridgeRoute, BridgeTransition<S,D> bridge, S currentState) {
		Preconditions.checkNotNull(currentState, "bridge, but current state is null");
		return Optional.of(State.of(bridgeRoute.destination(), bridge.apply(currentState)));
	}

	private static <S,D> Optional<State<D>> runEnd(EndTransition<S> end, S currentState) {
		Preconditions.checkNotNull(currentState, "end, but current state is null");
		end.accept(currentState);
		return Optional.empty();
	}
	
	private static <S,D> Optional<State<D>> runPartingResolved(PartingWay<S, D, D> route, PartingTransition<S, D, D> transition, S currentState) {
		Either<Optional<State<D>>, Optional<State<D>>> either = runParting(route, transition, currentState);
		return either.isLeft() ? either.left() : either.right();
	}
	
	private static <S,A,B> Either<Optional<State<A>>, Optional<State<B>>> runParting(PartingWay<S, A, B> route, PartingTransition<S, A, B> transition, S currentState) {
		Preconditions.checkNotNull(currentState, "parting, but current state is null");
		Either<A, B> either = transition.apply(currentState);
		return either.isLeft() 
				? Either.left(Optional.of(State.of(route.oneDestination(), either.left()))) 
				: Either.right(Optional.of(State.of(route.otherDestination(), either.right())));
	}

	public static ProcessEngineLike with(ProcessRoutes<SingleSource<?,?>> routes) {
		List<Route<?>> starts = routes.all().stream()
			.filter(r -> r instanceof Start)
			.collect(Collectors.toList());
		Preconditions.checkArgument(starts.size()==1, "more or less than one start found: %s",starts);
		
		Map<NamedType<?>, SingleSource<?,?>> sourceMap = routes.all().stream()
			.filter(r -> !(r instanceof Start))
			.collect(Collectors.toMap(r -> sourceOf(r), r -> r));
		
		return new ProcessEngineLike(routes, (Start<?>) starts.get(0), sourceMap);
	}

	private static <T> NamedType<T> sourceOf(SingleSource<T,?> route) {
		return route.start();
	}
}
