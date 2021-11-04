package de.flapdoodle.transition.routes;

import de.flapdoodle.transition.StateID;

import java.util.Set;

public abstract class RouteSources {

		private RouteSources() {
				// no instance
		}

		public static Set<StateID<?>> sources(Route<?> route) {
				if (route instanceof Start) return StateID.setOf();
				if (route instanceof HasSource) return StateID.setOf(((HasSource<?, ?>) route).start());

				if (route instanceof MergingJunction) {
						MergingJunction<?, ?, ?> mergingJunction = (MergingJunction<?, ?, ?>) route;
						return StateID.setOf(mergingJunction.left(), mergingJunction.right());
				}
				if (route instanceof Merge3Junction) {
						Merge3Junction<?, ?, ?, ?> merge3Junction = (Merge3Junction<?, ?, ?, ?>) route;
						return StateID.setOf(
								merge3Junction.left(),
								merge3Junction.middle(),
								merge3Junction.right());
				}

				throw new IllegalArgumentException("Not supported: " + route.getClass());
		}

}
