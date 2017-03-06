package de.flapdoodle.transition.initlike;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import de.flapdoodle.transition.NamedType;

public class InitListenerTest {

	@Test
	public void initListenerHelper() {
		AtomicReference<String> container=new AtomicReference<String>();
		
		InitListener listener = InitListener.typedBuilder()
			.onStateReached(NamedType.typeOf(String.class), s -> container.set(s))
			.onStateTearDown(NamedType.typeOf(String.class), s -> container.set(s))
			.build();
		
		listener.onStateReached(NamedType.typeOf(String.class), "hello");
		assertEquals("hello", container.get());
		
		listener.onStateTearDown(NamedType.typeOf(String.class), "world");
		assertEquals("world", container.get());
	}
}
