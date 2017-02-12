package de.flapdoodle.transition.processlike;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.immutables.value.Value;
import org.immutables.value.Value.Default;

import de.flapdoodle.transition.routes.Route;

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
	default BiConsumer<Route<?>, Object> onStateChangeFailed() {
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
