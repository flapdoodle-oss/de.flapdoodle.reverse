# a generic way to build an init-like system

When you start an applications then there are some preconditions that must be fulfilled. 
And if this application should stop garbage has to be cleaned. This library provides building blocks to implement
such applications. 

## Building Blocks

An init-like system is more or less a graph of dependencies. So we define our system as transitions between vertices. 
A vertex is definied by a type and an optional name:

```java
NamedType<String> stringType = NamedType.typeOf(String.class);
NamedType<String> stringTypeWithLabel = NamedType.typeOf("foo", String.class);
```

Following transition types are possible:

```java
Start<String> start;
Bridge<String, String> bridge;
MergingJunction<String, String, String> merge;
Merge3Junction<String, String, String, String> merge3;

start = Start.of(typeOf(String.class));
bridge = Bridge.of(typeOf("a", String.class), typeOf("b", String.class));
merge = MergingJunction.of(typeOf("left", String.class), typeOf("right", String.class),
    typeOf("merged", String.class));
merge3 = Merge3Junction.of(typeOf("left", String.class), typeOf("middle", String.class),
    typeOf("right", String.class), typeOf("merged", String.class));
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
InitRoutes<SingleDestination<?>> routes = InitRoutes.builder()
    .add(Start.of(typeOf(String.class)), () -> State.of("hello"))
    .build();

InitLike init = InitLike.with(routes);

try (InitLike.Init<String> state = init.init(typeOf(String.class))) {

  assertEquals("hello", state.current());

}

```

Our first dependency:

```java
InitRoutes<SingleDestination<?>> routes = InitRoutes.builder()
    .add(Start.of(typeOf(String.class)), () -> State.of("hello"))
    .add(Bridge.of(typeOf(String.class), typeOf("bridge", String.class)), s -> State.of(s + " world"))
    .build();

InitLike init = InitLike.with(routes);

try (InitLike.Init<String> state = init.init(typeOf("bridge", String.class))) {

  assertEquals("hello world", state.current());

}
```

Merging two dependencies:

```java
InitRoutes<SingleDestination<?>> routes = InitRoutes.builder()
    .add(Start.of(typeOf("hello", String.class)), () -> State.of("hello"))
    .add(Start.of(typeOf("again", String.class)), () -> State.of("again"))
    .add(Bridge.of(typeOf("hello", String.class), typeOf("bridge", String.class)), s -> State.of("[" + s + "]"))
    .add(
        MergingJunction.of(typeOf("bridge", String.class), typeOf("again", String.class),
            typeOf("merge", String.class)),
        (a, b) -> State.of(a + " " + b))
    .build();

InitLike init = InitLike.with(routes);

try (InitLike.Init<String> state = init.init(typeOf("merge", String.class))) {

  assertEquals("[hello] again", state.current());

}
```

If two is not enough:

```java
InitRoutes<SingleDestination<?>> routes = InitRoutes.builder()
    .add(Start.of(typeOf("hello", String.class)), () -> State.of("hello"))
    .add(Start.of(typeOf("again", String.class)), () -> State.of("again"))
    .add(Bridge.of(typeOf("hello", String.class), typeOf("bridge", String.class)), s -> State.of("[" + s + "]"))
    .add(Merge3Junction.of(typeOf("hello", String.class), typeOf("bridge", String.class),
        typeOf("again", String.class),
        typeOf("3merge", String.class)), (a, b, c) -> State.of(a + " " + b + " " + c))
    .build();

InitLike init = InitLike.with(routes);

try (InitLike.Init<String> state = init.init(typeOf("3merge", String.class))) {

  assertEquals("hello [hello] again", state.current());

}
```

The ordering of each entry does not matter. We only have to define our transitions, how to get to the destination is automatically resolved.
No transition is called twice and it is possible to work on an partial initialized system.

```java
InitRoutes<SingleDestination<?>> routes = InitRoutes.fluentBuilder()
    .given().state(String.class).isReachedBy(() -> State.of("hello", tearDownListener()))
    .given(String.class).state(typeOf("bridge", String.class))
    .isReachedBy(s -> State.of(s + " world", tearDownListener()))
    .build();

InitLike init = InitLike.with(routes);

try (InitLike.Init<String> state = init.init(typeOf(String.class))) {

  assertEquals("hello", state.current());

  try (InitLike.Init<String> subState = state.init(typeOf("bridge", String.class))) {

    assertEquals("hello world", subState.current());

  }
}
```

### Define a System (more verbose version)

In the beginning you need to create something out of noting.

```java
InitRoutes<SingleDestination<?>> routes = InitRoutes.fluentBuilder()
    .given().state(String.class).isInitializedWith("hello")
    .build();

InitLike init = InitLike.with(routes);

try (InitLike.Init<String> state = init.init(typeOf(String.class))) {

  assertEquals("hello", state.current());

}

```

Our first dependency:

```java
InitRoutes<SingleDestination<?>> routes = InitRoutes.fluentBuilder()
    .given().state(String.class).isInitializedWith("hello")
    .given(String.class).state(typeOf("bridge", String.class)).isReachedByMapping(s -> s + " world")
    .build();

InitLike init = InitLike.with(routes);

try (InitLike.Init<String> state = init.init(typeOf("bridge", String.class))) {

  assertEquals("hello world", state.current());

}
```

Merging two dependencies:

```java
NamedType<String> typeOfHello = typeOf("hello", String.class);
NamedType<String> typeOfAgain = typeOf("again", String.class);
NamedType<String> typeOfBridge = typeOf("bridge", String.class);
NamedType<String> typeOfMerge = typeOf("merge", String.class);

InitRoutes<SingleDestination<?>> routes = InitRoutes.fluentBuilder()
    .given().state(typeOfHello).isInitializedWith("hello")
    .given().state(typeOfAgain).isInitializedWith("again")
    .given(typeOfHello).state(typeOfBridge).isReachedByMapping(s -> "[" + s + "]")
    .given(typeOfBridge, typeOfAgain).state(typeOfMerge)
    .isReachedByMapping((a, b) -> a + " " + b)
    .build();

InitLike init = InitLike.with(routes);

try (InitLike.Init<String> state = init.init(typeOfMerge)) {

  assertEquals("[hello] again", state.current());

}
```

If two is not enough:

```java
NamedType<String> typeOfHello = typeOf("hello", String.class);
NamedType<String> typeOfAgain = typeOf("again", String.class);
NamedType<String> typeOfBridge = typeOf("bridge", String.class);
NamedType<String> typeOfMerge3 = typeOf("3merge", String.class);

InitRoutes<SingleDestination<?>> routes = InitRoutes.fluentBuilder()
    .given().state(typeOfHello).isInitializedWith("hello")
    .given().state(typeOfAgain).isInitializedWith("again")
    .given(typeOfHello).state(typeOfBridge).isReachedByMapping(s -> "[" + s + "]")
    .given(typeOfHello, typeOfBridge, typeOfAgain).state(typeOfMerge3)
    .isReachedBy((a, b, c) -> State.of(a + " " + b + " " + c))
    .build();

InitLike init = InitLike.with(routes);

try (InitLike.Init<String> state = init.init(typeOfMerge3)) {

  assertEquals("hello [hello] again", state.current());

}
```

The ordering of each entry does not matter. We only have to define our transitions, how to get to the destination is automatically resolved.
No transition is called twice and it is possible to work on an partial initialized system.

```java
InitRoutes<SingleDestination<?>> routes = InitRoutes.fluentBuilder()
    .given().state(String.class).isReachedBy(() -> State.of("hello", tearDownListener()))
    .given(String.class).state(typeOf("bridge", String.class))
    .isReachedBy(s -> State.of(s + " world", tearDownListener()))
    .build();

InitLike init = InitLike.with(routes);

try (InitLike.Init<String> state = init.init(typeOf(String.class))) {

  assertEquals("hello", state.current());

  try (InitLike.Init<String> subState = state.init(typeOf("bridge", String.class))) {

    assertEquals("hello world", subState.current());

  }
}
```


## Sample Application

... first we need a little helper:

[de.flapdoodle.java8 - Try](https://github.com/flapdoodle-oss/de.flapdoodle.java8/blob/master/src/main/java/de/flapdoodle/types/Try.java)

... create an temp directory

```java
InitRoutes<SingleDestination<?>> routes = InitRoutes.fluentBuilder()
    .given().state(Path.class).isReachedBy(() -> {
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


try (InitLike.Init<Path> state = init.init(typeOf(Path.class))) {
  Path currentTempDir = state.current();
...

}

```

... and create an file in this temp directory

```java
NamedType<Path> TEMP_DIR = typeOf("tempDir", Path.class);
NamedType<Path> TEMP_FILE = typeOf("tempFile", Path.class);

InitRoutes<SingleDestination<?>> routes = InitRoutes.fluentBuilder()
    .given().state(TEMP_DIR).isReachedBy(() -> {
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
NamedType<Path> TEMP_DIR = typeOf("tempDir", Path.class);
NamedType<Path> TEMP_FILE = typeOf("tempFile", Path.class);
NamedType<String> CONTENT = typeOf("content", String.class);

InitRoutes<SingleDestination<?>> routes = InitRoutes.fluentBuilder()
    .given().state(TEMP_DIR).isReachedBy(() -> {
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
    .given().state(CONTENT).isInitializedWith("hello world")
    .given(TEMP_FILE, CONTENT).state(typeOf("done", Boolean.class)).isReachedBy((tempFile, content) -> {
      Try
          .consumer((Path t) -> Files.write(t, "hello world".getBytes(Charset.defaultCharset())))
          .mapCheckedException(RuntimeException::new)
          .accept(tempFile);
      return State.of(true);
    })
    .build();

InitLike init = InitLike.with(routes);

try (InitLike.Init<Boolean> state = init.init(typeOf("done", Boolean.class))) {
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
