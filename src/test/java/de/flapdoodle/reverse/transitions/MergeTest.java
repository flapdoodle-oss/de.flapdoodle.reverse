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

public class MergeTest {

    @Test
    public void mergeFourStates() {
        StateID<String> firstState = StateID.of("first", String.class);
        StateID<String> secondState = StateID.of("second", String.class);
        StateID<String> thirdState = StateID.of("third", String.class);
        StateID<String> fourthState = StateID.of("fourth", String.class);
        StateID<String> mergedState = StateID.of("merged", String.class);

        Transition<String> toFirst = Start.to(firstState).initializedWith("A");
        Transition<String> toSecond = Start.to(secondState).initializedWith("B");
        Transition<String> toThird = Start.to(thirdState).initializedWith("C");
        Transition<String> toFourth = Start.to(fourthState).initializedWith("D");
        
        Transition<String> toMerged = Merge.given(firstState)
                .and(secondState)
                .and(thirdState)
                .and(fourthState)
                .state(mergedState)
                .deriveBy((a, b, c, d) -> a + b + c + d);

        Transitions transitions = Transitions.from(toFirst, toSecond, toThird, toFourth, toMerged);
        
        try (TransitionWalker.ReachedState<String> state = transitions.walker().initState(mergedState)) {
            assertThat(state.current()).isEqualTo("ABCD");
        }
    }

    @Test
    public void mergeWithExplicitStateConstruction() {
        StateID<String> firstState = StateID.of("first", String.class);
        StateID<String> secondState = StateID.of("second", String.class);
        StateID<String> thirdState = StateID.of("third", String.class);
        StateID<String> fourthState = StateID.of("fourth", String.class);
        StateID<String> mergedState = StateID.of("merged", String.class);

        Transition<String> toFirst = Start.to(firstState).initializedWith("A");
        Transition<String> toSecond = Start.to(secondState).initializedWith("B");
        Transition<String> toThird = Start.to(thirdState).initializedWith("C");
        Transition<String> toFourth = Start.to(fourthState).initializedWith("D");
        
        Transition<String> toMerged = Merge.given(firstState)
                .and(secondState)
                .and(thirdState)
                .and(fourthState)
                .state(mergedState)
                .with((a, b, c, d) -> State.of(a + "-" + b + "-" + c + "-" + d));

        Transitions transitions = Transitions.from(toFirst, toSecond, toThird, toFourth, toMerged);
        
        try (TransitionWalker.ReachedState<String> state = transitions.walker().initState(mergedState)) {
            assertThat(state.current()).isEqualTo("A-B-C-D");
        }
    }
    
    @Test
    public void mergeShouldAlwaysRebuildDestinationState() {
        StateID<Integer> firstState = StateID.of("first", Integer.class);
        StateID<Integer> secondState = StateID.of("second", Integer.class);
        StateID<Integer> thirdState = StateID.of("third", Integer.class);
        StateID<Integer> fourthState = StateID.of("fourth", Integer.class);
        StateID<Integer> mergedState = StateID.of("merged", Integer.class);

        Transition<Integer> toFirst = Start.to(firstState).initializedWith(1);
        Transition<Integer> toSecond = Start.to(secondState).initializedWith(2);
        Transition<Integer> toThird = Start.to(thirdState).initializedWith(3);
        Transition<Integer> toFourth = Start.to(fourthState).initializedWith(4);
        
        Transition<Integer> toMerged = Merge.given(firstState)
                .and(secondState)
                .and(thirdState)
                .and(fourthState)
                .state(mergedState)
                .deriveBy((a, b, c, d) -> a + b + c + d);

        Transitions transitions = Transitions.from(toFirst, toSecond, toThird, toFourth, toMerged);
        
        try (TransitionWalker.ReachedState<Integer> state = transitions.walker().initState(mergedState)) {
            assertThat(state.current()).isEqualTo(10);
        }
    }
    
    @Test
    public void mergeDifferentTypes() {
        StateID<Integer> firstState = StateID.of("count", Integer.class);
        StateID<String> secondState = StateID.of("name", String.class);
        StateID<Boolean> thirdState = StateID.of("valid", Boolean.class);
        StateID<Double> fourthState = StateID.of("value", Double.class);
        StateID<String> mergedState = StateID.of("result", String.class);

        Transition<Integer> toFirst = Start.to(firstState).initializedWith(42);
        Transition<String> toSecond = Start.to(secondState).initializedWith("test");
        Transition<Boolean> toThird = Start.to(thirdState).initializedWith(true);
        Transition<Double> toFourth = Start.to(fourthState).initializedWith(3.14);
        
        Transition<String> toMerged = Merge.given(firstState)
                .and(secondState)
                .and(thirdState)
                .and(fourthState)
                .state(mergedState)
                .deriveBy((count, name, valid, value) -> 
                    name + ":" + count + ":" + valid + ":" + value);

        Transitions transitions = Transitions.from(toFirst, toSecond, toThird, toFourth, toMerged);
        
        try (TransitionWalker.ReachedState<String> state = transitions.walker().initState(mergedState)) {
            assertThat(state.current()).isEqualTo("test:42:true:3.14");
        }
    }
}