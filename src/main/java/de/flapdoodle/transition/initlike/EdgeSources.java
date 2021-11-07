/**
 * Copyright (C) 2016
 *   Michael Mosmann <michael@mosmann.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.flapdoodle.transition.initlike;

import de.flapdoodle.transition.StateID;
import de.flapdoodle.transition.initlike.edges.Depends;
import de.flapdoodle.transition.initlike.edges.Merge2;
import de.flapdoodle.transition.initlike.edges.Merge3;
import de.flapdoodle.transition.initlike.edges.Start;

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
