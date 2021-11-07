package de.flapdoodle.transition.process;

import de.flapdoodle.transition.StateID;

public interface HasSource<S> {
		StateID<S> source();
}
