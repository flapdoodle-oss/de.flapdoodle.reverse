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
ThreeWayMergingJunction<String, String, String, String> merge3;

start = Start.of(typeOf(String.class));
bridge = Bridge.of(typeOf("a", String.class), typeOf("b", String.class));
merge = MergingJunction.of(typeOf("left",String.class), typeOf("right",String.class), typeOf("merged",String.class));
merge3 = ThreeWayMergingJunction.of(typeOf("left",String.class), typeOf("middle",String.class), typeOf("right",String.class), typeOf("merged",String.class));
```

The result of a transition must be wrapped into a `State`, which provides an optional tearDown hook:

```java
State<String> state = State.builder("hello")
  .onTearDown(value -> System.out.println("tearDown "+value))
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
    .add(MergingJunction.of(typeOf("bridge", String.class), typeOf("again", String.class), typeOf("merge", String.class)),
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
    .add(ThreeWayMergingJunction.of(typeOf("hello", String.class), typeOf("bridge", String.class), typeOf("again", String.class),
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
InitRoutes<SingleDestination<?>> routes = InitRoutes.builder()
    .add(Start.of(typeOf(String.class)), () -> State.of("hello", tearDownListener()))
    .add(Bridge.of(typeOf(String.class), typeOf("bridge", String.class)), s -> State.of(s + " world", tearDownListener()))
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

```java
public interface Try {
  
  public static <T,E extends Exception> T get(ThrowingSupplier<T, E> supplier) {
    return asSupplier(supplier).get();
  }
  
  public static <T,E extends Exception> void accept(ThrowingConsumer<T, E> consumer, T value) {
    asConsumer(consumer).accept(value);
  }
  
  public static <T,E extends Exception> Supplier<T> asSupplier(ThrowingSupplier<T, E> supplier) {
    return () -> {
      try {
        return supplier.get();
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    };
  }
  
  public static <T,E extends Exception> Consumer<T> asConsumer(ThrowingConsumer<T, E> consumer) {
    return t -> {
      try {
        consumer.accept(t);
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    };
  }
  
  interface ThrowingSupplier<T,E extends Exception> {
    T get() throws E;
  }
  
  interface ThrowingConsumer<T,E extends Exception> {
    void accept(T t) throws E;
  }
}
```

... create an temp directory

```java
InitRoutes<SingleDestination<?>> routes = InitRoutes.builder()
    .add(Start.of(typeOf(Path.class)), () -> {
      return State.builder(Try.get(() -> Files.createTempDirectory("init-howto")))
          .onTearDown(tempDir -> Try.accept((Path p) -> Files.deleteIfExists(p), tempDir))
          .build();
    })
    .build();

InitLike init = InitLike.with(routes);

...


try (InitLike.Init<Path> state = init.init(typeOf(Path.class))) {
  Path currentTempDir=state.current();
...

}

```

... and create an file in this temp directory

```java
NamedType<Path> TEMP_DIR = typeOf("tempDir",Path.class);
NamedType<Path> TEMP_FILE = typeOf("tempFile",Path.class);

InitRoutes<SingleDestination<?>> routes = InitRoutes.builder()
    .add(Start.of(TEMP_DIR), () -> {
      return State.builder(Try.get(() -> Files.createTempDirectory("init-howto")))
          .onTearDown(tempDir -> Try.accept((Path p) -> Files.deleteIfExists(p), tempDir))
          .build();
    })
    .add(Bridge.of(TEMP_DIR, TEMP_FILE), (Path tempDir) -> {
      Path tempFile = tempDir.resolve("test.txt");
      Try.accept(t -> Files.write(t, new byte[0]), tempFile);
      return State.builder(tempFile)
          .onTearDown(t -> Try.accept((Path p) -> Files.deleteIfExists(p), t))
          .build();
    })
    .build();

InitLike init = InitLike.with(routes);

try (InitLike.Init<Path> state = init.init(TEMP_FILE)) {
  Path currentTempFile=state.current();
...

}

```

... write content into this file.

```java
NamedType<Path> TEMP_DIR = typeOf("tempDir",Path.class);
NamedType<Path> TEMP_FILE = typeOf("tempFile",Path.class);
NamedType<String> CONTENT = typeOf("content", String.class);

InitRoutes<SingleDestination<?>> routes = InitRoutes.builder()
    .add(Start.of(TEMP_DIR), () -> {
      return State.builder(Try.get(() -> Files.createTempDirectory("init-howto")))
          .onTearDown(tempDir -> Try.accept((Path p) -> Files.deleteIfExists(p), tempDir))
          .build();
    })
    .add(Bridge.of(TEMP_DIR, TEMP_FILE), (Path tempDir) -> {
      Path tempFile = tempDir.resolve("test.txt");
      return State.builder(tempFile)
          .onTearDown(t -> Try.accept((Path p) -> Files.deleteIfExists(p), t))
          .build();
    })
    .add(Start.of(CONTENT), () -> State.of("hello world"))
    .add(MergingJunction.of(TEMP_FILE, CONTENT, typeOf("done", Boolean.class)), (tempFile, content) -> {
      Try.accept(t -> Files.write(t, "hello world".getBytes(Charset.defaultCharset())), tempFile);
      return State.of(true);
    })
    .build();

InitLike init = InitLike.with(routes);

try (InitLike.Init<Boolean> state = init.init(typeOf("done", Boolean.class))) {
  Boolean done = state.current();
  assertTrue(done);
}

String dotFile = RoutesAsGraph.routeGraphAsDot("sampleApp", RoutesAsGraph.asGraph(routes.all()));
```

... and generate an dot file for your application graph: 

```
digraph sampleApp {
  rankdir=LR;

  "tempDir:interface java.nio.file.Path"[ shape="rectangle", label="tempDir:Path" ];
  "tempFile:interface java.nio.file.Path"[ shape="rectangle", label="tempFile:Path" ];
  "content:class java.lang.String"[ shape="rectangle", label="content:String" ];
  "done:class java.lang.Boolean"[ shape="rectangle", label="done:Boolean" ];

  "tempDir:interface java.nio.file.Path" -> "tempFile:interface java.nio.file.Path"[ label="ImmutableBridge" ];
  "tempFile:interface java.nio.file.Path" -> "done:class java.lang.Boolean"[ label="ImmutableMergingJunction" ];
  "content:class java.lang.String" -> "done:class java.lang.Boolean"[ label="ImmutableMergingJunction" ];
}

```
