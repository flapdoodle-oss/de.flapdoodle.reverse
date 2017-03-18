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
package de.flapdoodle.transition.processlike;

import java.util.Optional;

import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

import de.flapdoodle.transition.routes.Route;

public interface ProcessListener extends ProcessOnStateChange, ProcessOnStateChangeFailedWithRetry {
	
	public static Helper.Builder builder() {
		return ImmutableHelper.builder();
	}
	
	public static ProcessListener noop() {
		return builder().build();
	}
	
	@Immutable
	interface Helper extends ProcessListener {
		@Default
		default ProcessOnStateChange onStateChange() {
			return (a,b) -> {};
		}
		
		@Default
		default ProcessOnStateChangeFailedWithRetry onStateChangeFailedWithRetry() {
			return (a,b) -> {};
		}
		
		@Override
		default void onStateChange(Optional<? extends State<?>> lastState, State<?> newState) {
			onStateChange().onStateChange(lastState, newState);
		}
		
		@Override
		default void onStateChangeFailedWithRetry(Route<?> currentRoute, Optional<? extends State<?>> lastState) {
			onStateChangeFailedWithRetry().onStateChangeFailedWithRetry(currentRoute, lastState);
		}
		
		interface Builder {
	    Builder onStateChange(ProcessOnStateChange onStateChange);
	    Builder onStateChangeFailedWithRetry(ProcessOnStateChangeFailedWithRetry onStateChangeFailed);
			ProcessListener build();
		}
	}
}
