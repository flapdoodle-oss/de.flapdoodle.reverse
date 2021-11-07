# a generic way to build an init-like system

When you start an applications then there are some preconditions that must be fulfilled. 
And if this application should stop garbage has to be cleaned. This library provides building blocks to implement
such applications. 

## Building Blocks

An init-like system is more or less a graph of dependencies. So we define our system as transitions between vertices. 
A vertex is definied by a type and an optional name:

```java
StateID<String> id = StateID.of(String.class);
StateID<String> idWithLabel = StateID.of("foo", String.class);
```

Following transition types are possible:

```java
Start<String> start;
Depends<String, String> depends;
Merge2<String, String, String> merge;
Merge3<String, String, String, String> merge3;

start = Start.of(StateID.of(String.class), () -> State.of(""));
depends = Depends.of(StateID.of("a", String.class), StateID.of("b", String.class), it -> State.of(it));
merge = Merge2.of(StateID.of("left", String.class), StateID.of("right", String.class),
    StateID.of("merged", String.class), (a, b) -> State.of(a + b));
merge3 = Merge3.of(StateID.of("left", String.class), StateID.of("middle", String.class),
    StateID.of("right", String.class), StateID.of("merged", String.class), (a, b, c) -> State.of(a + b + c));
```

The result of a transition must be wrapped into a `State`, which provides an optional tearDown hook:

```java
State<String> state = State.builder("hello")
    .onTearDown(value -> System.out.println("tearDown " + value))
    .build();
```

The tearDown is called if needed.

### Define a System

In the beginning you need to create something out of noting.

```java
List<Edge<?>> edges = Arrays.asList(
    Start.of(StateID.of(String.class), () -> State.of("hello"))
);

InitLike init = InitLike.with(edges);

try (InitLike.ReachedState<String> state = init.init(StateID.of(String.class))) {
    assertEquals("hello", state.current());
}

```

Our first dependency:

```java
List<Edge<?>> edges = Arrays.asList(
    Start.of(StateID.of(String.class), () -> State.of("hello")),
    Depends.of(StateID.of(String.class), StateID.of("depends", String.class), s -> State.of(s + " world"))
);

InitLike init = InitLike.with(edges);

try (InitLike.ReachedState<String> state = init.init(StateID.of("depends", String.class))) {
    assertEquals("hello world", state.current());
}
```

Merging two dependencies:

```java
List<Edge<?>> edges = Arrays.asList(
    Start.of(StateID.of("hello", String.class), () -> State.of("hello")),
    Start.of(StateID.of("again", String.class), () -> State.of("again")),
    Depends.of(StateID.of("hello", String.class), StateID.of("depends", String.class),
        s -> State.of("[" + s + "]")),

    Merge2.of(StateID.of("depends", String.class), StateID.of("again", String.class),
        StateID.of("merge", String.class),
        (a, b) -> State.of(a + " " + b))
);

InitLike init = InitLike.with(edges);

try (InitLike.ReachedState<String> state = init.init(StateID.of("merge", String.class))) {
    assertEquals("[hello] again", state.current());
}
```

If two is not enough:

```java
List<Edge<?>> edges = Arrays.asList(
    Start.of(StateID.of("hello", String.class), () -> State.of("hello")),
    Start.of(StateID.of("again", String.class), () -> State.of("again")),
    Depends.of(StateID.of("hello", String.class), StateID.of("depends", String.class),
        s -> State.of("[" + s + "]")),
    Merge3.of(StateID.of("hello", String.class), StateID.of("depends", String.class),
        StateID.of("again", String.class),
        StateID.of("3merge", String.class), (a, b, c) -> State.of(a + " " + b + " " + c))
);

InitLike init = InitLike.with(edges);

try (InitLike.ReachedState<String> state = init.init(StateID.of("3merge", String.class))) {
    assertEquals("hello [hello] again", state.current());
}
```

The ordering of each entry does not matter. We only have to define our transitions, how to get to the destination is automatically resolved.
No transition is called twice and it is possible to work on an partial initialized system.

```java
List<Edge<?>> edges = Arrays.asList(
    Start.of(StateID.of(String.class), () -> State.of("hello", tearDownListener())),
    Depends.of(StateID.of(String.class), StateID.of("depends", String.class), s -> State.of(s + " world", tearDownListener()))
);

InitLike init = InitLike.with(edges);

try (InitLike.ReachedState<String> state = init.init(StateID.of(String.class))) {
    assertEquals("hello", state.current());
    try (InitLike.ReachedState<String> subState = state.init(StateID.of("depends", String.class))) {
        assertEquals("hello world", subState.current());
    }
}
```


## Sample Application

... first we need a little helper:

[de.flapdoodle.java8 - Try](https://github.com/flapdoodle-oss/de.flapdoodle.java8/blob/master/src/main/java/de/flapdoodle/types/Try.java)

... create an temp directory

```java
List<Edge<?>> edges = Arrays.asList(
    Start.of(StateID.of(Path.class), () -> State.builder(Try
            .supplier(() -> Files.createTempDirectory("init-howto"))
            .mapCheckedException(RuntimeException::new)
            .get())
        .onTearDown(tempDir -> Try
            .consumer((Path p) -> Files.deleteIfExists(p))
            .mapCheckedException(RuntimeException::new)
            .accept(tempDir))
        .build())
);

InitLike init = InitLike.with(edges);

...


try (InitLike.ReachedState<Path> state = init.init(StateID.of(Path.class))) {
    Path currentTempDir = state.current();
...

}

```

... and create an file in this temp directory

```java
StateID<Path> TEMP_DIR = StateID.of("tempDir", Path.class);
StateID<Path> TEMP_FILE = StateID.of("tempFile", Path.class);

List<Edge<?>> edges = Arrays.asList(
    Start.of(TEMP_DIR, () -> State.builder(Try
            .supplier(() -> Files.createTempDirectory("init-howto"))
            .mapCheckedException(RuntimeException::new)
            .get())
        .onTearDown(tempDir -> Try.consumer((Path p) -> Files.deleteIfExists(p))
            .mapCheckedException(RuntimeException::new)
            .accept(tempDir))
        .build()),
    Depends.of(TEMP_DIR, TEMP_FILE, (Path tempDir) -> {
        Path tempFile = tempDir.resolve("test.txt");
        Try.consumer((Path t) -> Files.write(t, new byte[0]))
            .mapCheckedException(RuntimeException::new)
            .accept(tempFile);
        return State.builder(tempFile)
            .onTearDown(t -> Try.consumer((Path p) -> Files.deleteIfExists(p))
                .mapCheckedException(RuntimeException::new)
                .accept(t))
            .build();
    })
);

InitLike init = InitLike.with(edges);

try (InitLike.ReachedState<Path> state = init.init(TEMP_FILE)) {
    Path currentTempFile = state.current();
...

}

```

... write content into this file.

```java
StateID<Path> TEMP_DIR = StateID.of("tempDir", Path.class);
StateID<Path> TEMP_FILE = StateID.of("tempFile", Path.class);
StateID<String> CONTENT = StateID.of("content", String.class);

List<Edge<?>> edges = Arrays.asList(
    Start.of(TEMP_DIR, () -> State.builder(Try
            .supplier(() -> Files.createTempDirectory("init-howto"))
            .mapCheckedException(RuntimeException::new)
            .get())
        .onTearDown(tempDir -> Try
            .consumer((Path p) -> Files.deleteIfExists(p))
            .mapCheckedException(RuntimeException::new)
            .accept(tempDir))
        .build()),
    Depends.of(TEMP_DIR, TEMP_FILE, (Path tempDir) -> {
        Path tempFile = tempDir.resolve("test.txt");
        return State.builder(tempFile)
            .onTearDown(t -> Try
                .consumer((Path p) -> Files.deleteIfExists(p))
                .mapCheckedException(RuntimeException::new)
                .accept(t))
            .build();
    }),
    Start.of(CONTENT, () -> State.of("hello world")),
    Merge2.of(TEMP_FILE, CONTENT, StateID.of("done", Boolean.class), (tempFile, content) -> {
        Try
            .consumer((Path t) -> Files.write(t, "hello world".getBytes(Charset.defaultCharset())))
            .mapCheckedException(RuntimeException::new)
            .accept(tempFile);
        return State.of(true);
    })
);

InitLike init = InitLike.with(edges);

try (InitLike.ReachedState<Boolean> state = init.init(StateID.of("done", Boolean.class))) {
    Boolean done = state.current();
    assertTrue(done);
}

String dotFile = EdgesAsGraph.edgeGraphAsDot("sampleApp",
    EdgesAsGraph.asGraphIncludingStartAndEnd(edges));
```

... and generate an dot file for your application graph: 

```
digraph sampleApp {
  rankdir=LR;

  "tempDir:interface java.nio.file.Path"[ shape="rectangle", label="tempDir:Path" ];
  "start_1:class java.lang.Void"[ shape="circle", label="" ];
  "tempFile:interface java.nio.file.Path"[ shape="rectangle", label="tempFile:Path" ];
  "content:class java.lang.String"[ shape="rectangle", label="content:String" ];
  "start_2:class java.lang.Void"[ shape="circle", label="" ];
  "done:class java.lang.Boolean"[ shape="rectangle", label="done:Boolean" ];

  "start_1:class java.lang.Void" -> "tempDir:interface java.nio.file.Path"[ label="Start" ];
  "tempDir:interface java.nio.file.Path" -> "tempFile:interface java.nio.file.Path"[ label="Depends" ];
  "start_2:class java.lang.Void" -> "content:class java.lang.String"[ label="Start" ];
  "tempFile:interface java.nio.file.Path" -> "done:class java.lang.Boolean"[ label="Merge2" ];
  "content:class java.lang.String" -> "done:class java.lang.Boolean"[ label="Merge2" ];
}

```
