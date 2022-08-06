# Why use Transitions

If you have to test some IO related problems you may find yourself in an situation where you have to create some
temporary files, and if you dont want to leave the garbage behind, to delete everything after the test is done:

```java
Path filePath = tempDir.resolve("some-file");

try {
  Files.write(filePath, "content".getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);

  Assertions.assertThat(filePath).exists().content().isEqualTo("content");
} finally {
  Files.deleteIfExists(filePath);
}

Assertions.assertThat(filePath).doesNotExist();
```

.. since java 7 you can wrap some if this into an Closable so that you can use the try-with-resources pattern:

```java
StateID<Path> basePath = StateID.of("basePath", Path.class);
StateID<String> fileName = StateID.of("fileName", String.class);
StateID<Path> pathOfFile = StateID.of("filePath", Path.class);
StateID<Path> writtenFilePath = StateID.of("writtenFilePath", Path.class);

Start<String> toFileName = Start.to(fileName).initializedWith("some-file");
Start<Path> toTempDir = Start.to(basePath).initializedWith(tempDir);
Join<Path, String, Path> toFilePath = Join.given(basePath).and(fileName).state(pathOfFile)
  .deriveBy(Path::resolve);

Derive<Path, Path> toWrittenFilePath = Derive.given(pathOfFile).state(writtenFilePath).with(path -> {
  try {
    Path result = Files.write(path, "some other content".getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);
    return State.of(result, current -> {
      try {
        Files.deleteIfExists(current);
      }
      catch (IOException ix) {
        throw new RuntimeException("Files.deleteIfExists", ix);
      }
    });
  }
  catch (IOException ix) {
    throw new RuntimeException("Files.write", ix);
  }
});

Transitions transitions = Transitions.from(toFileName, toTempDir, toFilePath, toWrittenFilePath);

Path filePath;
try (TransitionWalker.ReachedState<Path> writtenFile = transitions.walker().initState(writtenFilePath)) {
  filePath = writtenFile.current();
  Assertions.assertThat(writtenFile.current()).exists().content().isEqualTo("some other content");
}

Assertions.assertThat(filePath).doesNotExist();
```

As this example is not very complicated, even this looks a little bit like over-engineered.
But we can even go further (don't be afraid, in the end i hope you understand why we are doing this).

```java
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
  Assertions.assertThat(writeFile.file).exists().content().isEqualTo("other content");
}

Assertions.assertThat(filePath).doesNotExist();
```

TODO
* special classes
* bundle patterns (interfaces with default methods, builder-pattern, ...)
