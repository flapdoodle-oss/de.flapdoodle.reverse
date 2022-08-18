package de.flapdoodle.reverse;

import de.flapdoodle.reverse.transitions.*;
import de.flapdoodle.testdoc.Recorder;
import de.flapdoodle.testdoc.Recording;
import de.flapdoodle.testdoc.TabSize;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

public class WhyUseTransitionsTest {

	@RegisterExtension
	public static Recording recording = Recorder.with("WhyUseTransitions.md", TabSize.spaces(2));

	@Test
	public void startProblem(@TempDir Path tempDir) {
		recording.begin();
		Path baseDir = mkDir(tempDir, "sub");
		assertThat(baseDir).exists();

		try {
			Path source = writeFile(baseDir, "src", "source");
			assertThat(source).exists().content(StandardCharsets.UTF_8).isEqualTo("source");
			try {

				Path destination = copy(source, baseDir, "dst");
				assertThat(destination).exists().content(StandardCharsets.UTF_8).isEqualTo("source");

				// POINT OF RETURN

				delete(destination);
				assertThat(baseDir.resolve("dst")).doesNotExist();
			} finally {
				delete(source);
				assertThat(baseDir.resolve("src")).doesNotExist();
			}
		} finally {
			delete(baseDir);
			assertThat(tempDir.resolve("sub")).doesNotExist();
		}
		recording.end();
	}

	@Test
	public void autoCleanUp(@TempDir Path tempDir) {
		recording.begin();
		class Wrapper<T> implements AutoCloseable {
			final T value;
			private final Consumer<T> onClose;

			Wrapper(T value, Consumer<T> onClose) {
				this.value = value;
				this.onClose = onClose;
			}

			@Override
			public void close() {
				onClose.accept(value);
			}
		}
		recording.end();

		recording.begin();
		try (Wrapper<Path> baseDir = new Wrapper<>(mkDir(tempDir, "sub"), this::delete)) {
			assertThat(baseDir.value).exists();
			try (Wrapper<Path> source = new Wrapper<>(writeFile(baseDir.value, "src", "source"), this::delete)) {
				assertThat(source.value).exists().content(StandardCharsets.UTF_8).isEqualTo("source");
				try (Wrapper<Path> destination = new Wrapper<>(copy(source.value,baseDir.value,"dst"), this::delete)) {
					assertThat(destination.value).exists().content(StandardCharsets.UTF_8).isEqualTo("source");

					// POINT OF RETURN
					
				}
				assertThat(baseDir.value.resolve("dst")).doesNotExist();
			}
			assertThat(baseDir.value.resolve("src")).doesNotExist();
		}
		assertThat(tempDir.resolve("sub")).doesNotExist();
		recording.end();
	}

	@Test
	public void useTransitions(@TempDir Path tempDirValue) {
		recording.begin("stateIds");
		StateID<Path> tempDir = StateID.of("tempDir", Path.class);
		StateID<Path> baseDir = StateID.of("basePath", Path.class);
		StateID<Path> source = StateID.of("src", Path.class);
		StateID<Path> destination = StateID.of("dst", Path.class);
		recording.end();

		recording.begin("transitions");
		Transition<Path> toTempDir = Start.to(tempDir).initializedWith(tempDirValue);

		Transition<Path> toBaseDir = Derive.given(tempDir)
			.state(baseDir)
			.with(t -> State.of(mkDir(t, "sub"), this::delete));

		Transition<Path> writeSrc = Derive.given(baseDir)
			.state(source)
			.with(b -> State.of(writeFile(b,"src","source"), this::delete));

		Transition<Path> copySrcToDest = Join.given(source)
			.and(baseDir)
			.state(destination)
			.with((s,b) -> State.of(copy(s, b,"dst"), this::delete));
		recording.end();

		recording.begin("graph");
		Transitions transitions = Transitions.from(toTempDir, toBaseDir, writeSrc, copySrcToDest);
		recording.end();

		recording.begin("usage");
		try (TransitionWalker.ReachedState<Path> withBaseDir = transitions.walker().initState(baseDir)) {
			assertThat(withBaseDir.current()).exists();
			try (TransitionWalker.ReachedState<Path> withSource = withBaseDir.initState(source)) {
				assertThat(withSource.current()).exists().content(StandardCharsets.UTF_8).isEqualTo("source");
				try (TransitionWalker.ReachedState<Path> withDestination = withSource.initState(destination)) {
					assertThat(withDestination.current()).exists().content(StandardCharsets.UTF_8).isEqualTo("source");

					// POINT OF RETURN

				}
				assertThat(withBaseDir.current().resolve("dst")).doesNotExist();
			}
			assertThat(withBaseDir.current().resolve("src")).doesNotExist();
		}
		assertThat(tempDirValue.resolve("sub")).doesNotExist();
		recording.end();

		recording.begin("second-usage");
		try (TransitionWalker.ReachedState<Path> withDestination = transitions.walker().initState(destination)) {
			assertThat(withDestination.current()).exists().content(StandardCharsets.UTF_8).isEqualTo("source");

			assertThat(tempDirValue.resolve("sub")).exists();
		}
		assertThat(tempDirValue.resolve("sub")).doesNotExist();
		recording.end();

		recording.begin("export-dot");
		String dotFile = Transitions.edgeGraphAsDot("copy-file", transitions.asGraph());
		recording.end();
		recording.output("copy-file.dot", dotFile);
	}

	private static Path copy(Path source, Path base, String name) {
		try {
			return Files.copy(source, base.resolve(name));
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static Path writeFile(Path base, String name, String content) {
		try {
			return Files.write(base.resolve(name), content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static Path mkDir(Path base, String name) {
		try {
			return Files.createDirectory(base.resolve(name));
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void delete(Path path) {
		try {
			Files.delete(path);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
