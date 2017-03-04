# a generic way to build an init-like system

When you start an applications then there are some preconditions that must be fulfilled. 
And if this application should stop garbage has to be cleaned. This library provides building blocks to implement
such applications. 

## How to Start

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