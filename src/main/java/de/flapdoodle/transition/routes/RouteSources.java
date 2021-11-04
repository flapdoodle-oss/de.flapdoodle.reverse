package de.flapdoodle.transition.routes;

import de.flapdoodle.checks.Preconditions;
import de.flapdoodle.transition.StateID;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class RouteSources {

		private RouteSources() {
				// no instance
		}

		public static Set<StateID<?>> sources(Route<?> route) {
				if (route instanceof Start) return setOf();
				if (route instanceof HasSource) return setOf(((HasSource<?,?>) route).start());
				if (route instanceof MergingJunction) return setOf(
						((MergingJunction<?, ?, ?>) route).left(),
						((MergingJunction<?, ?, ?>) route).right()
				);
				if (route instanceof Merge3Junction) return setOf(
						((Merge3Junction<?, ?, ?, ?>) route).left(),
						((Merge3Junction<?, ?, ?, ?>) route).middle(),
						((Merge3Junction<?, ?, ?, ?>) route).right()
				);

				throw new IllegalArgumentException("Not supported: "+route.getClass());
		}

		private static Set<StateID<?>> setOf(StateID<?>...stateIds) {
				return Stream.of(stateIds).collect(Collectors.toSet());
		}
}
