package de.flapdoodle.transition.processlike;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

import de.flapdoodle.transition.NamedType;
import de.flapdoodle.transition.processlike.exceptions.RetryException;
import de.flapdoodle.transition.routes.Bridge;
import de.flapdoodle.transition.routes.End;
import de.flapdoodle.transition.routes.Route;
import de.flapdoodle.transition.routes.Start;

public class ProcessEngineLikeTest {

	@Test
	public void simpleSample() {
		ProcessRoutes<Route<?>> routes = ProcessRoutes.builder()
				.add(Start.of(typeOf(String.class)), () -> "12")
				.add(Bridge.of(typeOf(String.class), typeOf(Integer.class)), a -> Integer.valueOf(a))
				.add(End.of(typeOf(Integer.class)), i -> {})
				.build();

		ProcessEngineLike pe = ProcessEngineLike.with(routes);
		
		ProcessListener listener = new ProcessListener() {
			
			@Override
			public <T> long onStateChangeFailed(Route<?> route, NamedType<T> type, T state) {
				System.out.println("failed "+route+" -> "+type+"="+state);
				return 0;
			}
			
			@Override
			public <T> void onStateChange(Object oldState, NamedType<T> type, T newState) {
				System.out.println("changed "+oldState+" -> "+type+"="+newState);
			}
		};
		
		pe.run(listener);
		
	}
	
	@Test
	public void retrySample() {
		AtomicLong lastTimestamp=new AtomicLong(System.currentTimeMillis());
		
		ProcessRoutes<Route<?>> routes = ProcessRoutes.builder()
				.add(Start.of(typeOf(String.class)), () -> "12")
				.add(Bridge.of(typeOf(String.class), typeOf(Integer.class)), a -> {
					long current=System.currentTimeMillis();
					long last = lastTimestamp.get();
					long diff = current-last;
					System.out.println("Diff: "+diff);
					if (diff<3) {
						throw new RetryException("diff is :"+diff);
					}
					lastTimestamp.set(current);
					return Integer.valueOf(a);
				})
				.add(End.of(typeOf(Integer.class)), i -> {})
				.build();

		ProcessEngineLike pe = ProcessEngineLike.with(routes);
		
		ProcessListener listener = new ProcessListener() {
			
			@Override
			public <T> long onStateChangeFailed(Route<?> route, NamedType<T> type, T state) {
				System.out.println("failed "+route+" -> "+type+"="+state);
				return 3;
			}
			
			@Override
			public <T> void onStateChange(Object oldState, NamedType<T> type, T newState) {
				System.out.println("changed "+oldState+" -> "+type+"="+newState);
			}
		};
		
		pe.run(listener);
	}
	
	private static String asString(Object value) {
		return value!=null ? value+"("+value.getClass()+")" : "null";
	}

	private static <T> NamedType<T> typeOf(Class<T> type) {
		return NamedType.typeOf(type);
	}

	private static <T> NamedType<T> typeOf(String name, Class<T> type) {
		return NamedType.typeOf(name, type);
	}

}
