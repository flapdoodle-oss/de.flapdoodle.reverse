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
package de.flapdoodle.reverse.transitions;

import de.flapdoodle.reverse.State;
import de.flapdoodle.reverse.StateID;
import de.flapdoodle.reverse.Transition;
import de.flapdoodle.reverse.TransitionWalker;
import de.flapdoodle.reverse.Transitions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CombineTest {

    @Test
    public void combineThreeStates() {
        StateID<String> firstState = StateID.of("first", String.class);
        StateID<String> secondState = StateID.of("second", String.class);
        StateID<String> thirdState = StateID.of("third", String.class);
        StateID<String> combinedState = StateID.of("combined", String.class);

        Transition<String> toFirst = Start.to(firstState).initializedWith("A");
        Transition<String> toSecond = Start.to(secondState).initializedWith("B");
        Transition<String> toThird = Start.to(thirdState).initializedWith("C");
        
        Transition<String> toCombined = Combine.given(firstState)
                .and(secondState)
                .and(thirdState)
                .state(combinedState)
                .deriveBy((a, b, c) -> a + b + c);

        Transitions transitions = Transitions.from(toFirst, toSecond, toThird, toCombined);
        
        try (TransitionWalker.ReachedState<String> state = transitions.walker().initState(combinedState)) {
            assertThat(state.current()).isEqualTo("ABC");
        }
    }

    @Test
    public void combineWithExplicitStateConstruction() {
        StateID<String> firstState = StateID.of("first", String.class);
        StateID<String> secondState = StateID.of("second", String.class);
        StateID<String> thirdState = StateID.of("third", String.class);
        StateID<String> combinedState = StateID.of("combined", String.class);

        Transition<String> toFirst = Start.to(firstState).initializedWith("A");
        Transition<String> toSecond = Start.to(secondState).initializedWith("B");
        Transition<String> toThird = Start.to(thirdState).initializedWith("C");
        
        Transition<String> toCombined = Combine.given(firstState)
                .and(secondState)
                .and(thirdState)
                .state(combinedState)
                .with((a, b, c) -> State.of(a + "-" + b + "-" + c));

        Transitions transitions = Transitions.from(toFirst, toSecond, toThird, toCombined);
        
        try (TransitionWalker.ReachedState<String> state = transitions.walker().initState(combinedState)) {
            assertThat(state.current()).isEqualTo("A-B-C");
        }
    }
    
    @Test
    public void combineShouldAlwaysRebuildDestinationState() {
        StateID<Integer> firstState = StateID.of("first", Integer.class);
        StateID<Integer> secondState = StateID.of("second", Integer.class);
        StateID<Integer> thirdState = StateID.of("third", Integer.class);
        StateID<Integer> combinedState = StateID.of("combined", Integer.class);

        Transition<Integer> toFirst = Start.to(firstState).initializedWith(1);
        Transition<Integer> toSecond = Start.to(secondState).initializedWith(2);
        Transition<Integer> toThird = Start.to(thirdState).initializedWith(3);
        
        Transition<Integer> toCombined = Combine.given(firstState)
                .and(secondState)
                .and(thirdState)
                .state(combinedState)
                .deriveBy((a, b, c) -> a + b + c);

        Transitions transitions = Transitions.from(toFirst, toSecond, toThird, toCombined);
        
        try (TransitionWalker.ReachedState<Integer> state = transitions.walker().initState(combinedState)) {
            assertThat(state.current()).isEqualTo(6);
        }
    }
}