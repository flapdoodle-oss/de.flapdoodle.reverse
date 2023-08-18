/*
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
package de.flapdoodle.reverse;

import de.flapdoodle.checks.Preconditions;
import de.flapdoodle.reverse.graph.*;
import org.immutables.value.Value;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.*;
import java.util.stream.Collectors;

@Value.Immutable
public abstract class Transitions {

	public abstract List<Transition<?>> transitions();

	@Value.Auxiliary
	public Transitions addAll(Transition<?> ...transitions) {
		return ImmutableTransitions.builder()
			.from(this)
			.addTransitions(transitions)
			.build();
	}

	public Transitions addAll(Transitions other) {
		return ImmutableTransitions.builder()
			.from(this)
			.addAllTransitions(other.transitions())
			.build();
	}

	@Value.Auxiliary
	public <T> Transitions replace(Transition<T> transition) {
		List<Transition<?>> filteredTransition = transitions().stream()
			.filter(it -> !it.destination().equals(transition.destination()))
			.collect(Collectors.toList());

		boolean transitionWithSameDestinationRemoved = filteredTransition.size() + 1 == transitions().size();

		Preconditions.checkArgument(transitionWithSameDestinationRemoved,"no transition with destination %s found", transition.destination());

		return ImmutableTransitions.builder()
			.transitions(filteredTransition)
			.addTransitions(transition)
			.build();
	}

	@Value.Check
	protected void checkForCollisions() {
		assertNoCollisions(transitions());
	}

	@Value.Auxiliary
	public TransitionWalker walker() {
		return TransitionWalker.with(transitions());
	}

	public static Transitions from(Transition<?> ... transitions){
		return ImmutableTransitions.builder().addTransitions(transitions).build();
	}

	public static void assertNoCollisions(List<? extends Transition<?>> all) {
		String transitionWithCollisions = all.stream()
			.collect(Collectors.groupingBy(Transition::destination))
			.entrySet().stream().filter(entry -> entry.getValue().size() > 1)
			.map(entry -> entry.getKey() + " --> "+entry.getValue())
			.collect(Collectors.joining(",\n  "));

		Preconditions.checkArgument(transitionWithCollisions.isEmpty(), "multiple transitions with same destination: \n  %s", transitionWithCollisions);
	}
}
