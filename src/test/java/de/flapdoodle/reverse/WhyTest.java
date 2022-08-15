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

import de.flapdoodle.reverse.transitions.Derive;
import de.flapdoodle.reverse.transitions.Join;
import de.flapdoodle.reverse.transitions.Start;
import de.flapdoodle.testdoc.Recorder;
import de.flapdoodle.testdoc.Recording;
import de.flapdoodle.testdoc.TabSize;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

public class WhyTest {

	@RegisterExtension
	public static Recording recording = Recorder.with("WhyUseTransitions.md", TabSize.spaces(2));

	@Test
	public void startProblem(@TempDir Path tempDir) throws IOException {
		recording.begin();
		Path filePath = tempDir.resolve("some-file");

		try {
			Files.write(filePath, "content".getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);

			assertThat(filePath).exists().content().isEqualTo("content");
		} finally {
			Files.deleteIfExists(filePath);
		}

		assertThat(filePath).doesNotExist();
		recording.end();
	}

	@Test
	public void changeToTryWithResources(@TempDir Path tempDir) throws IOException {
		recording.begin();
		class WriteFile implements Closeable {
			private final Path file;

			WriteFile(Path base, String name, String content) throws IOException {
				file = Files.write(base.resolve(name), content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);
			}

			@Override
			public void close() throws IOException {
				Files.deleteIfExists(file);
			}
		}

		Path filePath;
		try (WriteFile writeFile = new WriteFile(tempDir, "some-file", "other content")) {
			filePath = writeFile.file;
			assertThat(writeFile.file).exists().content().isEqualTo("other content");
		}

		assertThat(filePath).doesNotExist();
		recording.end();
	}

	@Test
	public void useTransitions(@TempDir Path tempDir) {
		recording.begin();
		StateID<Path> basePath = StateID.of("basePath", Path.class);
		StateID<String> fileName = StateID.of("fileName", String.class);
		StateID<Path> pathOfFile = StateID.of("filePath", Path.class);
		StateID<Path> writtenFilePath = StateID.of("writtenFilePath", Path.class);

		Start<String> toFileName = Start.to(fileName).initializedWith("some-file");
		Start<Path> toTempDir = Start.to(basePath).initializedWith(tempDir);
		Join<Path, String, Path> toFilePath = Join.given(basePath).and(fileName).state(pathOfFile)
			.deriveBy(Path::resolve);

		Derive<Path, Path> toWrittenFilePath = Derive.given(pathOfFile).state(writtenFilePath)
			.with(
				ThrowingFunction.wrap(path -> State.of(
					Files.write(path, "some other content".getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW),
					TearDown.wrap(ThrowingConsumer.wrap(Files::deleteIfExists))
				))
			);

		Transitions transitions = Transitions.from(toFileName, toTempDir, toFilePath, toWrittenFilePath);

		Path filePath;
		try (TransitionWalker.ReachedState<Path> writtenFile = transitions.walker().initState(writtenFilePath)) {
			filePath = writtenFile.current();
			assertThat(writtenFile.current()).exists().content().isEqualTo("some other content");
		}

		assertThat(filePath).doesNotExist();
		recording.end();
	}

	@Test
	public void explain(@TempDir Path tempDir) throws IOException {
		recording.begin();
		State<Path> wrappedValue = State.of(tempDir.resolve("some-file"));

		Path writtenFile = Files.write(wrappedValue.value(), "content".getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);
//		State.of(writtenFile, current -> {
//			Files.deleteIfExists(current);
//		});

		recording.end();
	}
}
