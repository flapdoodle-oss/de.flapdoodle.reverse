package de.flapdoodle.transition.initlike;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Lazy;
import org.immutables.value.Value.Parameter;

import de.flapdoodle.transition.NamedType;

public interface InitListener extends InitOnStateReached, InitOnStateTearDown {
	
	public static TypedListener.Builder typedBuilder() {
		return ImmutableTypedListener.builder();
	}
	
	public static ImmutableSimple.Builder builder() {
		return ImmutableSimple.builder();
	}
	
	public static InitListener of(BiConsumer<NamedType<?>, Object> onStateReached, BiConsumer<NamedType<?>, Object> onTearDown) {
		return builder()
				.onStateReached(onStateReached)
				.onTearDown(onTearDown)
				.build();
	}
	
	@Immutable
	abstract class Simple implements InitListener {
		protected abstract Optional<BiConsumer<NamedType<?>, Object>> onStateReached();
		protected abstract Optional<BiConsumer<NamedType<?>, Object>> onTearDown();
		
		@Override
		public <T> void onStateReached(NamedType<T> stateName, T value) {
			onStateReached().ifPresent(l -> l.accept(stateName, value));
		}
		
		@Override
		public <T> void onStateTearDown(NamedType<T> stateName, T value) {
			onTearDown().ifPresent(l -> l.accept(stateName, value));
		}
	}
	
	@Immutable
	abstract class TypedListener implements InitListener {
		
		protected abstract List<StateListener<?>> stateReachedListener();
		protected abstract List<StateListener<?>> stateTearDownListener();
		
		@Auxiliary
		@Lazy
		protected Map<NamedType<?>, Consumer<?>> stateReachedListenerAsMap() {
			return stateReachedListener().stream()
					.collect(Collectors.toMap(l -> l.type(), l -> l.listener()));
		}
		
		@Auxiliary
		@Lazy
		protected Map<NamedType<?>, Consumer<?>> stateTearDownListenerAsMap() {
			return stateTearDownListener().stream()
					.collect(Collectors.toMap(l -> l.type(), l -> l.listener()));
		}
		
		@Override
		public <T> void onStateReached(NamedType<T> stateName, T value) {
			Optional.ofNullable((Consumer<T>) stateReachedListenerAsMap().get(stateName))
				.ifPresent(c -> c.accept(value));
		}
		
		@Override
		public <T> void onStateTearDown(NamedType<T> stateName, T value) {
			Optional.ofNullable((Consumer<T>) stateTearDownListenerAsMap().get(stateName))
				.ifPresent(c -> c.accept(value));
		}
		
		interface Builder {
			Builder addStateReachedListener(StateListener<?> listener);
			Builder addStateTearDownListener(StateListener<?> listener);
			
	    default <T> Builder onStateReached(NamedType<T> type, Consumer<T> listener) {
	    	return addStateReachedListener(StateListener.of(type, listener));
	    }
	    default <T> Builder onStateTearDown(NamedType<T> type, Consumer<T> listener) {
	    	return addStateTearDownListener(StateListener.of(type, listener));
	    }
	    InitListener build();
		}

	}

	@Immutable
	interface StateListener<T> {
		@Parameter
		NamedType<T> type();
		@Parameter
		Consumer<T> listener();
		
		public static <T> StateListener<T> of(NamedType<T> type, Consumer<T> listener) {
			return ImmutableStateListener.of(type, listener);
		}
	}
	
}
