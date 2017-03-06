package de.flapdoodle.transition.initlike;

import de.flapdoodle.transition.NamedType;

public interface InitOnStateTearDown {
	<T> void onStateTearDown(NamedType<T> stateName, T value);
}
