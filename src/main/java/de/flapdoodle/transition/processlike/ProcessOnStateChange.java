package de.flapdoodle.transition.processlike;

import java.util.Optional;

public interface ProcessOnStateChange {
	void onStateChange(Optional<? extends State<?>> lastState, State<?> newState);

}
