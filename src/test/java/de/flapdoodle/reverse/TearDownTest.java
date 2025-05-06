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

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class TearDownTest {

    @Test
    public void shouldPropagateFirstExceptionWhenBothMethodsThrow() {
        // Given
        RuntimeException firstException = new RuntimeException("first");
        RuntimeException secondException = new RuntimeException("second");
        
        TearDown<String> first = s -> { throw firstException; };
        TearDown<String> second = s -> { throw secondException; };
        
        // When / Then
        TearDown<String> combined = first.andThen(second);
        
        Exception caught = assertThrows(RuntimeException.class, () -> combined.onTearDown("test"));
        assertSame(firstException, caught);
        
        Throwable[] suppressed = caught.getSuppressed();
        assertEquals(1, suppressed.length);
        assertSame(secondException, suppressed[0]);
    }
    
    @Test
    public void shouldPropagateOnlyFirstExceptionWhenOnlyFirstMethodThrows() {
        // Given
        RuntimeException firstException = new RuntimeException("first");
        AtomicBoolean secondCalled = new AtomicBoolean(false);
        
        TearDown<String> first = s -> { throw firstException; };
        TearDown<String> second = s -> { secondCalled.set(true); };
        
        // When / Then
        TearDown<String> combined = first.andThen(second);
        
        Exception caught = assertThrows(RuntimeException.class, () -> combined.onTearDown("test"));
        assertSame(firstException, caught);
        assertTrue(secondCalled.get(), "Second teardown should be called even when first throws");
        assertEquals(0, caught.getSuppressed().length, "Should have no suppressed exceptions");
    }
    
    @Test
    public void shouldPropagateOnlySecondExceptionWhenOnlySecondMethodThrows() {
        // Given
        RuntimeException secondException = new RuntimeException("second");
        AtomicBoolean firstCalled = new AtomicBoolean(false);
        
        TearDown<String> first = s -> { firstCalled.set(true); };
        TearDown<String> second = s -> { throw secondException; };
        
        // When / Then
        TearDown<String> combined = first.andThen(second);
        
        Exception caught = assertThrows(RuntimeException.class, () -> combined.onTearDown("test"));
        assertSame(secondException, caught);
        assertTrue(firstCalled.get(), "First teardown should be called");
        assertEquals(0, caught.getSuppressed().length, "Should have no suppressed exceptions");
    }
    
    @Test
    public void shouldExecuteBothMethodsWhenNoExceptionsThrown() {
        // Given
        AtomicInteger callCount = new AtomicInteger(0);
        
        TearDown<String> first = s -> { callCount.incrementAndGet(); };
        TearDown<String> second = s -> { callCount.incrementAndGet(); };
        
        // When
        TearDown<String> combined = first.andThen(second);
        combined.onTearDown("test");
        
        // Then
        assertEquals(2, callCount.get(), "Both teardown methods should be called");
    }
}