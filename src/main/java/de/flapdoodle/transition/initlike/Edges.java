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
import java.util.function.Function;

public class Edges {
		private Edges() {
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

		public static <D> Function<StateLookup, State<D>> actionHandler(Edge<D> edge) {
				if (edge instanceof Start) return startActionHandler((Start<D>) edge);
				if (edge instanceof Depends) return dependsActionHandler((Depends<?, D>) edge);
				if (edge instanceof Merge2) return merge2ActionHandler((Merge2<?, ?, D>) edge);
				if (edge instanceof Merge3) return merge3ActionHandler((Merge3<?, ?, ?, D>) edge);

				throw new IllegalArgumentException("not supported: "+edge);
		}
		private static <D> Function<StateLookup, State<D>> startActionHandler(Start<D> edge) {
				return lookup -> edge.action().get();
		}

		private static <S, D> Function<StateLookup, State<D>> dependsActionHandler(Depends<S, D> edge) {
				return lookup -> edge.action().apply(lookup.of(edge.source()));
		}

		private static <L, R, D> Function<StateLookup, State<D>> merge2ActionHandler(Merge2<L, R, D> edge) {
				return lookup -> edge.action().apply(lookup.of(edge.left()), lookup.of(edge.right()));
		}
		private static <L, M, R, D> Function<StateLookup, State<D>> merge3ActionHandler(Merge3<L, M, R, D> edge) {
				return lookup -> edge.action().apply(lookup.of(edge.left()), lookup.of(edge.middle()), lookup.of(edge.right()));
		}
}
