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
package de.flapdoodle.reverse.doc;

import de.flapdoodle.reverse.*;
import de.flapdoodle.reverse.graph.TransitionGraph;
import de.flapdoodle.reverse.transitions.Start;
import de.flapdoodle.testdoc.Includes;
import de.flapdoodle.testdoc.Recorder;
import de.flapdoodle.testdoc.Recording;
import de.flapdoodle.testdoc.TabSize;
import de.flapdoodle.types.Try;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;

import static org.assertj.core.api.Assertions.assertThat;

public class SummaryTest {

	@RegisterExtension
	public static Recording recording = Recorder.with("Summary.md", TabSize.spaces(2))
		.sourceCodeOf("Hashing.java", Hashing.class, Includes.Trim, Includes.WithoutImports, Includes.WithoutPackage)
		.sourceCodeOf("FileFactory.java", FileFactory.class, Includes.Trim, Includes.WithoutImports, Includes.WithoutPackage);

	@Test
	public void intro(@TempDir Path tempDir) throws IOException, NoSuchAlgorithmException {
		recording.begin();
		Path result = Files.write(tempDir.resolve("testee.txt"), "some content".getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);
		try {
			String hash = Hashing.hash(result);
			assertThat(hash).isEqualTo("290f493c44f5d63d06b374d0a5abd292fae38b92cab2fae5efefe1b0e9347f56");
		}
		finally {
			Files.delete(result);
			assertThat(result).doesNotExist();
		}
		recording.end();
	}

	@Test
	public void abstractFileCreation(@TempDir Path tempDir) throws IOException, NoSuchAlgorithmException {
		recording.begin();
		AutoCloseableWrapper<Path, IOException> result = FileFactory.createTemporaryFileWithContent(tempDir.resolve("testee.txt"), "some content");
		try {
			String hash = Hashing.hash(result.wrapped());
			assertThat(hash).isEqualTo("290f493c44f5d63d06b374d0a5abd292fae38b92cab2fae5efefe1b0e9347f56");
		}
		finally {
			result.close();
			assertThat(result.wrapped()).doesNotExist();
		}
		recording.end();
	}

	@Test
	public void tryWithResources(@TempDir Path tempDir) throws IOException, NoSuchAlgorithmException {
		recording.begin();
		try (AutoCloseableWrapper<Path, IOException> result = FileFactory.createTemporaryFileWithContent(tempDir.resolve("testee.txt"), "some content")) {
			String hash = Hashing.hash(result.wrapped());
			assertThat(hash).isEqualTo("290f493c44f5d63d06b374d0a5abd292fae38b92cab2fae5efefe1b0e9347f56");
		}
		recording.end();
	}

	@Test
	public void startTransition(@TempDir Path tempDir) throws IOException, NoSuchAlgorithmException {
		recording.begin();
		Start<Path> transition = Start.to(Path.class)
			.with(() -> State.of(
				FileFactory.createFileWithContent(tempDir.resolve("testee.txt"), "some content"),
				it -> Try.run(() -> Files.delete(it)))
			);

		State<Path> result = transition.result(new StateLookup() {
			@Override public <D> D of(StateID<D> type) {
				throw new IllegalArgumentException("must not be called");
			}
		});

		try {
			String hash = Hashing.hash(result.value());
			assertThat(hash).isEqualTo("290f493c44f5d63d06b374d0a5abd292fae38b92cab2fae5efefe1b0e9347f56");
		}
		finally {
			result.onTearDown()
				.ifPresent(tearDown -> tearDown.onTearDown(result.value()));
			assertThat(result.value()).doesNotExist();
		}
		recording.end();
	}

	@Test
	public void transitions(@TempDir Path tempDir) throws IOException, NoSuchAlgorithmException {
		recording.begin();
		Start<Path> transition = Start.to(Path.class)
			.with(() -> State.of(
				FileFactory.createFileWithContent(tempDir.resolve("testee.txt"), "some content"),
				it -> Try.run(() -> Files.delete(it)))
			);

		try (TransitionWalker.ReachedState<Path> result = Transitions.from(transition)
			.walker()
			.initState(StateID.of(Path.class))) {
			String hash = Hashing.hash(result.current());
			assertThat(hash).isEqualTo("290f493c44f5d63d06b374d0a5abd292fae38b92cab2fae5efefe1b0e9347f56");
		}
		recording.end();
		String dot = TransitionGraph.edgeGraphAsDot("transitions", Transitions.from(transition));
		recording.file("transitions.dot", "Summary.svg", GraphvizAdapter.asSvg(dot));
	}
}
