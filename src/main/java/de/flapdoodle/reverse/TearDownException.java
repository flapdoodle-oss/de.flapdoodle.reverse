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
package de.flapdoodle.reverse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TearDownException extends RuntimeException {

	private final List<RuntimeException> exceptions;

	public TearDownException(String message, List<RuntimeException> exceptions) {
		super(message);
		this.exceptions = new ArrayList<>(exceptions);
		this.exceptions.forEach(this::addSuppressed);
	}

	public TearDownException(String message, RuntimeException cause) {
		super(message, cause);
		this.exceptions = new ArrayList<>();
		this.exceptions.add(cause);
	}

	public List<RuntimeException> getExceptions() {
		return Collections.unmodifiableList(exceptions);
	}

}
