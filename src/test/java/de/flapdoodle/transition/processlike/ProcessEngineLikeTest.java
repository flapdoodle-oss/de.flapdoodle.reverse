package de.flapdoodle.transition.processlike;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

import de.flapdoodle.transition.NamedType;
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
		
		ImmutableProcessEngineListener listener = ProcessEngineListener.builder()
				.beforeStart(() -> System.out.println("starting"))
				.onStateChange((last,current) -> System.out.println("state change "+asString(last)+" -> "+asString(current)))
				.afterEnd((last) -> System.out.println("ending with "+asString(last)))
				.build();
		
		pe.run(listener, new SimpleLimitedRetryWaitTimeCalculator(1, 100));
		
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
					if (diff<20) {
						throw new RetryException("diff is :"+diff);
					}
					lastTimestamp.set(current);
					return Integer.valueOf(a);
				})
				.add(End.of(typeOf(Integer.class)), i -> {})
				.build();

		ProcessEngineLike pe = ProcessEngineLike.with(routes);
		
		ImmutableProcessEngineListener listener = ProcessEngineListener.builder()
				.beforeStart(() -> System.out.println("starting"))
				.onStateChange((last,current) -> System.out.println("state change "+asString(last)+" -> "+asString(current)))
				.onStateChangeFailed((route,current) -> System.out.println("state change failed "+route+" -> "+asString(current)))
				.afterEnd((last) -> System.out.println("ending with "+asString(last)))
				.build();
		
		SimpleLimitedRetryWaitTimeCalculator retry = new SimpleLimitedRetryWaitTimeCalculator(1, 100);
		
		for (int i=0;i<10;i++) {
			pe.run(listener, retry);
		}
		
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
