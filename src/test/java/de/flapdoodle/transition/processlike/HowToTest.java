package de.flapdoodle.transition.processlike;

import static de.flapdoodle.transition.NamedType.typeOf;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.ClassRule;
import org.junit.Test;

import de.flapdoodle.testdoc.Includes;
import de.flapdoodle.testdoc.Recorder;
import de.flapdoodle.testdoc.Recording;
import de.flapdoodle.testdoc.TabSize;
import de.flapdoodle.transition.NamedType;
import de.flapdoodle.transition.Try;
import de.flapdoodle.transition.routes.Bridge;
import de.flapdoodle.transition.routes.End;
import de.flapdoodle.transition.routes.PartingWay;
import de.flapdoodle.transition.routes.Route;
import de.flapdoodle.transition.routes.RoutesAsGraph;
import de.flapdoodle.transition.routes.SingleSource;
import de.flapdoodle.transition.routes.Start;
import de.flapdoodle.transition.types.Either;

public class HowToTest {
	@ClassRule
	public static Recording recording = Recorder.with("HowToBuildAnProcessEngine.md", TabSize.spaces(2))
		.sourceCodeOf("try", Try.class, Includes.WithoutImports, Includes.WithoutPackage, Includes.Trim);
	
	@Test
	public void vertex() {
		recording.begin();
		NamedType<String> stringType = NamedType.typeOf(String.class);
		NamedType<String> stringTypeWithLabel = NamedType.typeOf("foo", String.class);
		recording.end();
	}
	
	@Test
	public void edges() {
		recording.begin();
		Start<String> start;
		Bridge<String, String> bridge;
		PartingWay<String, String, String> parting;
		End<String> end;
		
		start = Start.of(typeOf(String.class));
		bridge = Bridge.of(typeOf("a", String.class), typeOf("b", String.class));
		parting = PartingWay.of(typeOf("start", String.class), typeOf("oneDestination", String.class), typeOf("otherDestination", String.class));
		end = End.of(typeOf("start", String.class));
		recording.end();
	}
	
	@Test
	public void state() {
		recording.begin();
		State<String> state = State.of(typeOf("foo", String.class), "hello");
		recording.end();
	}

	@Test
	public void listener() {
		recording.begin();
		ProcessListener listener=ProcessListener.builder()
				.onStateChange((Optional<? extends State<?>> route, State<?> currentState) -> {
					
				})
				.onStateChangeFailedWithRetry((Route<?> currentRoute, Optional<? extends State<?>> lastState) -> {
					// decide, if thread should sleep some time
				})
				.build();
		recording.end();
	}
	
	@Test
	public void startAndEnd() {
		AtomicReference<String> result=new AtomicReference<>();
		List<State<?>> states=new ArrayList<>();
		
		recording.begin();
		ProcessRoutes<SingleSource<?,?>> routes = ProcessRoutes.builder()
				.add(Start.of(typeOf(String.class)), () -> "foo")
				.add(End.of(typeOf(String.class)), i -> { result.set(i); })
				.build();

		ProcessEngineLike pe = ProcessEngineLike.with(routes);
		
		ProcessListener listener = ProcessListener.builder()
				.onStateChange((route, currentState) -> {
					states.add(currentState);
				})
				.onStateChangeFailedWithRetry((currentRoute, lastState) -> {
					throw new IllegalArgumentException("should not happen");
				})
				.build();
		
		pe.run(listener);
		recording.end();
		
		assertEquals("foo",result.get());
		assertEquals(1,states.size());
		assertEquals("foo",states.get(0).value());
	}
	
	@Test
	public void startBridgeAndEnd() {
		AtomicReference<Integer> result=new AtomicReference<>();
		recording.begin();
		ProcessRoutes<SingleSource<?,?>> routes = ProcessRoutes.builder()
				.add(Start.of(typeOf(String.class)), () -> "12")
				.add(Bridge.of(typeOf(String.class), typeOf(Integer.class)), a -> Integer.valueOf(a))
				.add(End.of(typeOf(Integer.class)), i -> { result.set(i); })
				.build();

		ProcessEngineLike pe = ProcessEngineLike.with(routes);
		
		pe.run(ProcessListener.noop());
		recording.end();
		
		assertEquals(Integer.valueOf(12),result.get());
	}
	
	@Test
	public void loopSample() {
		List<Object> values=new ArrayList<>();
		
		recording.begin();
		ProcessRoutes<SingleSource<?,?>> routes = ProcessRoutes.builder()
				.add(Start.of(typeOf("start", Integer.class)), () -> 0)
				.add(Bridge.of(typeOf("start", Integer.class), typeOf("decide", Integer.class)), a -> a+1)
				.add(PartingWay.of(typeOf("decide", Integer.class), typeOf("start", Integer.class), typeOf("end", Integer.class)), a -> a<3 ? Either.left(a) : Either.right(a))
				.add(End.of(typeOf("end", Integer.class)), i -> {
					values.add(i);
				})
				.build();
		
		ProcessEngineLike pe = ProcessEngineLike.with(routes);
		
		ProcessListener listener = ProcessListener.builder()
				.onStateChange((route, currentState) -> {
					if (currentState.type().name().equals("decide")) {
						values.add(currentState.value());
					}
				})
				.build();
		
		pe.run(listener);

		String dot = RoutesAsGraph.routeGraphAsDot("simpleLoop", RoutesAsGraph.asGraphIncludingStartAndEnd(routes.all()));
		recording.end();
		
		recording.output("dotFile", dot);
		
		assertEquals("[1, 2, 3, 3]", values.toString());
	}


}
