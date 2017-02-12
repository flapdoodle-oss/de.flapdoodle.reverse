package de.flapdoodle.transition.processlike;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import de.flapdoodle.transition.NamedType;
import de.flapdoodle.transition.Preconditions;
import de.flapdoodle.transition.processlike.transitions.BridgeTransition;
import de.flapdoodle.transition.processlike.transitions.EndTransition;
import de.flapdoodle.transition.processlike.transitions.StartTransition;
import de.flapdoodle.transition.routes.Bridge;
import de.flapdoodle.transition.routes.End;
import de.flapdoodle.transition.routes.Route;
import de.flapdoodle.transition.routes.Route.Transition;
import de.flapdoodle.transition.routes.Start;

public class ProcessEngineLike {

	private final ProcessRoutes<Route<?>> routes;
	private final Start<?> start;
	private final Map<NamedType<?>, Route<?>> sourceMap;

	private ProcessEngineLike(ProcessRoutes<Route<?>> routes, Start<?> start, Map<NamedType<?>, Route<?>> sourceMap) {
		this.routes = Preconditions.checkNotNull(routes,"routes is null");
		this.start = Preconditions.checkNotNull(start,"start is null");
		this.sourceMap = new LinkedHashMap<>(Preconditions.checkNotNull(sourceMap,"sourceMap is null"));
	}
	
	public void run(ProcessEngineListener listener, HistoryBasedRetryWaitTime retryTime) {
		Object currentState = null;
		Route<?> currentRoute = start;
		
		Optional<State<?>> newState=Optional.empty();
		
		listener.beforeStart().run();
		long currentRetryTime=0;
		
		do {
			try {
				newState = run(currentRoute, currentState);
				if (newState.isPresent()) {
					currentRoute = sourceMap.get(newState.get().type);
					Object newStateValue = newState.get().value;
					listener.onStateChange().accept(currentState, newStateValue);
					currentState = newStateValue;
				}
				currentRetryTime = retryTime.waitTime(currentRetryTime, true);
			} catch (RetryException rx) {
				listener.onStateChangeFailed().accept(currentRoute, currentState);
				currentRetryTime = retryTime.waitTime(currentRetryTime, false);
				sleep(currentRetryTime);
			}
		} while (newState.isPresent());
		
		listener.afterEnd().accept(currentState);
	}

	private void sleep(long currentRetryTime) {
		try {
			System.out.println("sleeping "+currentRetryTime);
			Thread.sleep(currentRetryTime);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		}
	}
	

	private Optional<State<?>> run(Route<?> currentRoute, Object currentState) {
		Transition<?> transition = routes.transitionOf(currentRoute);
		if (transition instanceof StartTransition) {
			Preconditions.checkArgument(currentState==null, "starting, but current state: %s",currentState);
			StartTransition<?> start=(StartTransition<?>) transition;
			Start startRoute = (Start) currentRoute;
			return Optional.of(State.of(startRoute.destination(), start.get()));
		}
		if (transition instanceof BridgeTransition) {
			Preconditions.checkNotNull(currentState, "bridge, but current state is null");
			BridgeTransition bridge=(BridgeTransition<?,?>) transition;
			Bridge bridgeRoute = (Bridge) currentRoute;
			return Optional.of(State.of(bridgeRoute.destination(), bridge.apply(currentState)));
		}
		if (transition instanceof EndTransition) {
			Preconditions.checkNotNull(currentState, "end, but current state is null");
			EndTransition end=(EndTransition<?>) transition;
			end.accept(currentState);
			return Optional.empty();
		}
		
		throw new IllegalArgumentException(""+currentRoute+": could not run "+transition);
	}

	public static ProcessEngineLike with(ProcessRoutes<Route<?>> routes) {
		List<Route<?>> starts = routes.all().stream()
			.filter(r -> r instanceof Start)
			.collect(Collectors.toList());
		Preconditions.checkArgument(starts.size()==1, "more or less than one start found: %s",starts);
		
		Map<NamedType<?>, Route<?>> sourceMap = routes.all().stream()
			.filter(r -> !(r instanceof Start))
			.collect(Collectors.toMap(r -> sourceOf(r), r -> r));
		
		return new ProcessEngineLike(routes, (Start) starts.get(0), sourceMap);
	}

	private static <T> NamedType<T> sourceOf(Route<T> route) {
		if (route instanceof Bridge) {
			return ((Bridge) route).start();
		}
		if (route instanceof End) {
			return ((End) route).start();
		}
		throw new IllegalArgumentException("could not get source of "+route);
	}
	
	static class State<T> {
		private final NamedType<T> type;
		private final T value;

		private State(NamedType<T> type, T value) {
			this.type = type;
			this.value = value;
		}
		
		public static <T> State<T> of(NamedType<T> type, T value) {
			return new State<T>(type, value);
		}
	}
}
