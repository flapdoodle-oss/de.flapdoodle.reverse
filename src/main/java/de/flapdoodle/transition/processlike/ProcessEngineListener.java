package de.flapdoodle.transition.processlike;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.immutables.value.Value;
import org.immutables.value.Value.Default;

@Value.Immutable
public interface ProcessEngineListener {
	@Default
	default Runnable beforeStart() {
		return () -> {};
	}
	
	@Default
	default BiConsumer<Object, Object> onStateChange() {
		return (a,b) -> {};
	}
	
	@Default
	default Consumer<Object> afterEnd() {
		return (a) -> {};
	}
	
	public static ImmutableProcessEngineListener.Builder builder() {
		return ImmutableProcessEngineListener.builder();
	}
}
