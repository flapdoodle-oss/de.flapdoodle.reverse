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
Bridge<String, String> bridge;
MergingJunction<String, String, String> merge;
Merge3Junction<String, String, String, String> merge3;

start = Start.of(StateID.of(String.class));
bridge = Bridge.of(StateID.of("a", String.class), StateID.of("b", String.class));
merge = MergingJunction.of(StateID.of("left", String.class), StateID.of("right", String.class),
    StateID.of("merged", String.class));
merge3 = Merge3Junction.of(StateID.of("left", String.class), StateID.of("middle", String.class),
    StateID.of("right", String.class), StateID.of("merged", String.class));
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
InitRoutes<HasDestination<?>> routes = InitRoutes.builder()
    .state(String.class).isInitializedWith("hello")
    .build();

InitLike init = InitLike.with(routes);

try (InitLike.Init<String> state = init.init(StateID.of(String.class))) {

  assertEquals("hello", state.current());

}

```

Our first dependency:

```java
InitRoutes<HasDestination<?>> routes = InitRoutes.builder()
    .state(String.class).isInitializedWith("hello")
    .given(String.class).state(StateID.of("bridge", String.class)).isDerivedBy(s -> s + " world")
    .build();

InitLike init = InitLike.with(routes);

try (InitLike.Init<String> state = init.init(StateID.of("bridge", String.class))) {

  assertEquals("hello world", state.current());

}
```

Merging two dependencies:

```java
StateID<String> hello = StateID.of("hello", String.class);
StateID<String> again = StateID.of("again", String.class);
StateID<String> mappedHello = StateID.of("mapped", String.class);
StateID<String> result = StateID.of("result", String.class);

InitRoutes<HasDestination<?>> routes = InitRoutes.builder()
    .state(hello).isInitializedWith("hello")
    .state(again).isInitializedWith("again")
    .given(hello).state(mappedHello).isDerivedBy(s -> "[" + s + "]")
    .given(mappedHello, again).state(result)
    .isDerivedBy((a, b) -> a + " " + b)
    .build();

InitLike init = InitLike.with(routes);

try (InitLike.Init<String> state = init.init(result)) {

  assertEquals("[hello] again", state.current());

}
```

If two is not enough:

```java
StateID<String> hello = StateID.of("hello", String.class);
StateID<String> again = StateID.of("again", String.class);
StateID<String> mapped = StateID.of("mapped", String.class);
StateID<String> result = StateID.of("result", String.class);

InitRoutes<HasDestination<?>> routes = InitRoutes.builder()
    .state(hello).isInitializedWith("hello")
    .state(again).isInitializedWith("again")
    .given(hello).state(mapped).isDerivedBy(s -> "[" + s + "]")
    .given(hello, mapped, again).state(result)
    .isDerivedBy((a, b, c) -> a + " " + b + " " + c)
    .build();

InitLike init = InitLike.with(routes);

try (InitLike.Init<String> state = init.init(result)) {

  assertEquals("hello [hello] again", state.current());

}
```

The ordering of each entry does not matter. We only have to define our transitions, how to get to the destination is automatically resolved.
No transition is called twice and it is possible to work on an partial initialized system.

```java
InitRoutes<HasDestination<?>> routes = InitRoutes.builder()
    .state(String.class).isReachedBy(() -> State.of("hello", tearDownListener()))
    .given(String.class).state(StateID.of("bridge", String.class))
    .isReachedBy(s -> State.of(s + " world", tearDownListener()))
    .build();

InitLike init = InitLike.with(routes);

try (InitLike.Init<String> state = init.init(StateID.of(String.class))) {

  assertEquals("hello", state.current());

  try (InitLike.Init<String> subState = state.init(StateID.of("bridge", String.class))) {

    assertEquals("hello world", subState.current());

  }
}
```


## Sample Application

... first we need a little helper:

[de.flapdoodle.java8 - Try](https://github.com/flapdoodle-oss/de.flapdoodle.java8/blob/master/src/main/java/de/flapdoodle/types/Try.java)

... create an temp directory

```java
InitRoutes<HasDestination<?>> routes = InitRoutes.builder()
    .state(Path.class).isReachedBy(() -> {
      return State.builder(Try
          .supplier(() -> Files.createTempDirectory("init-howto"))
          .mapCheckedException(RuntimeException::new)
          .get())
          .onTearDown(tempDir -> Try
              .consumer((Path p) -> Files.deleteIfExists(p))
              .mapCheckedException(RuntimeException::new)
              .accept(tempDir))
          .build();
    })
    .build();

InitLike init = InitLike.with(routes);

...


try (InitLike.Init<Path> state = init.init(StateID.of(Path.class))) {
  Path currentTempDir = state.current();
...

}

```

... and create an file in this temp directory

```java
StateID<Path> TEMP_DIR = StateID.of("tempDir", Path.class);
StateID<Path> TEMP_FILE = StateID.of("tempFile", Path.class);

InitRoutes<HasDestination<?>> routes = InitRoutes.builder()
    .state(TEMP_DIR).isReachedBy(() -> {
      return State.builder(Try
          .supplier(() -> Files.createTempDirectory("init-howto"))
          .mapCheckedException(RuntimeException::new)
          .get())
          .onTearDown(tempDir -> Try.consumer((Path p) -> Files.deleteIfExists(p))
              .mapCheckedException(RuntimeException::new)
              .accept(tempDir))
          .build();
    })
    .given(TEMP_DIR).state(TEMP_FILE).isReachedBy((Path tempDir) -> {
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
    .build();

InitLike init = InitLike.with(routes);

try (InitLike.Init<Path> state = init.init(TEMP_FILE)) {
  Path currentTempFile = state.current();
...

}

```

... write content into this file.

```java
StateID<Path> TEMP_DIR = StateID.of("tempDir", Path.class);
StateID<Path> TEMP_FILE = StateID.of("tempFile", Path.class);
StateID<String> CONTENT = StateID.of("content", String.class);

InitRoutes<HasDestination<?>> routes = InitRoutes.builder()
    .state(TEMP_DIR).isReachedBy(() -> {
      return State.builder(Try
          .supplier(() -> Files.createTempDirectory("init-howto"))
          .mapCheckedException(RuntimeException::new)
          .get())
          .onTearDown(tempDir -> Try
              .consumer((Path p) -> Files.deleteIfExists(p))
              .mapCheckedException(RuntimeException::new)
              .accept(tempDir))
          .build();
    })
    .given(TEMP_DIR).state(TEMP_FILE).isReachedBy((Path tempDir) -> {
      Path tempFile = tempDir.resolve("test.txt");
      return State.builder(tempFile)
          .onTearDown(t -> Try
              .consumer((Path p) -> Files.deleteIfExists(p))
              .mapCheckedException(RuntimeException::new)
              .accept(t))
          .build();
    })
    .state(CONTENT).isInitializedWith("hello world")
    .given(TEMP_FILE, CONTENT).state(StateID.of("done", Boolean.class)).isReachedBy((tempFile, content) -> {
      Try
          .consumer((Path t) -> Files.write(t, "hello world".getBytes(Charset.defaultCharset())))
          .mapCheckedException(RuntimeException::new)
          .accept(tempFile);
      return State.of(true);
    })
    .build();

InitLike init = InitLike.with(routes);

try (InitLike.Init<Boolean> state = init.init(StateID.of("done", Boolean.class))) {
  Boolean done = state.current();
  assertTrue(done);
}

String dotFile = RoutesAsGraph.routeGraphAsDot("sampleApp",
    RoutesAsGraph.asGraphIncludingStartAndEnd(routes.all()));
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
  "tempDir:interface java.nio.file.Path" -> "tempFile:interface java.nio.file.Path"[ label="Bridge" ];
  "start_2:class java.lang.Void" -> "content:class java.lang.String"[ label="Start" ];
  "content:class java.lang.String" -> "done:class java.lang.Boolean"[ label="MergingJunction" ];
  "tempFile:interface java.nio.file.Path" -> "done:class java.lang.Boolean"[ label="MergingJunction" ];
}

```
