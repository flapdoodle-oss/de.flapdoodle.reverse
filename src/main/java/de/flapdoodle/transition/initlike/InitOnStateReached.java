package de.flapdoodle.transition.initlike;

import de.flapdoodle.transition.NamedType;

public interface InitOnStateReached {
	<T> void onStateReached(NamedType<T> stateName, T value);
}
