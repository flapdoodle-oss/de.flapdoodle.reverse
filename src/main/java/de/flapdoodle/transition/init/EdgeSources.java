package de.flapdoodle.transition.init;

import de.flapdoodle.transition.StateID;
import de.flapdoodle.transition.init.edges.Depends;
import de.flapdoodle.transition.init.edges.Merge2;
import de.flapdoodle.transition.init.edges.Merge3;
import de.flapdoodle.transition.init.edges.Start;

import java.util.Set;

public class EdgeSources {
		private EdgeSources() {
				// no instance
		}

		public static Set<StateID<?>> sources(Edge<?> route) {
				if (route instanceof Start) return StateID.setOf();
				if (route instanceof Depends) return StateID.setOf(((Depends<?, ?>) route).source());

				if (route instanceof Merge2) {
						Merge2<?, ?, ?> mergingJunction = (Merge2<?, ?, ?>) route;
						return StateID.setOf(mergingJunction.left(), mergingJunction.right());
				}
				if (route instanceof Merge3) {
						Merge3<?, ?, ?, ?> merge3Junction = (Merge3<?, ?, ?, ?>) route;
						return StateID.setOf(
								merge3Junction.left(),
								merge3Junction.middle(),
								merge3Junction.right());
				}

				throw new IllegalArgumentException("Not supported: " + route.getClass());
		}

}
