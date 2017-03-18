# a generic way to build an process engine

This library provides building blocks to implement a simple process engine. 

## Building Blocks

An process engine is more or less a graph of dependencies. So we define our system as transitions between vertices. 
A vertex is definied by a type and an optional name:

```java
NamedType<String> stringType = NamedType.typeOf(String.class);
NamedType<String> stringTypeWithLabel = NamedType.typeOf("foo", String.class);
```

Following transition types are possible:

```java
Start<String> start;
Bridge<String, String> bridge;
PartingWay<String, String, String> parting;
End<String> end;

start = Start.of(typeOf(String.class));
bridge = Bridge.of(typeOf("a", String.class), typeOf("b", String.class));
parting = PartingWay.of(typeOf("start", String.class), typeOf("oneDestination", String.class), typeOf("otherDestination", String.class));
end = End.of(typeOf("start", String.class));
```

The result of a transition is wrapped into a `State` visible by a process listener:

```java
State<String> state = State.of(typeOf("foo", String.class), "hello");
```

You can listen to events with an ProcessListener:

```java
ProcessListener listener=ProcessListener.builder()
    .onStateChange((Optional<? extends State<?>> route, State<?> currentState) -> {
      
    })
    .onStateChangeFailedWithRetry((Route<?> currentRoute, Optional<? extends State<?>> lastState) -> {
      // decide, if thread should sleep some time
    })
    .build();
```


### Define a System

In the beginning you need to create something out of noting and end end wich resolves to nothing.

```java
ProcessRoutes<SingleSource<?,?>> routes = ProcessRoutes.builder()
    .add(Start.of(typeOf(String.class)), () -> "foo")
    .add(End.of(typeOf(String.class)), i -> { result.set(i); })
    .build();

ProcessEngineLike pe = ProcessEngineLike.with(routes);

ProcessListener listener = ProcessListener.builder()
    .onStateChange((route, currentState) -> {
      states.add(currentState);
    })
    .onStateChangeFailedWithRetry((currentRoute, lastState) -> {
      throw new IllegalArgumentException("should not happen");
    })
    .build();

pe.run(listener);
```

Transformation in between:

```java
ProcessRoutes<SingleSource<?,?>> routes = ProcessRoutes.builder()
    .add(Start.of(typeOf(String.class)), () -> "12")
    .add(Bridge.of(typeOf(String.class), typeOf(Integer.class)), a -> Integer.valueOf(a))
    .add(End.of(typeOf(Integer.class)), i -> { result.set(i); })
    .build();

ProcessEngineLike pe = ProcessEngineLike.with(routes);

pe.run(ProcessListener.noop());
```

Simple looping process:

```java
ProcessRoutes<SingleSource<?,?>> routes = ProcessRoutes.builder()
    .add(Start.of(typeOf("start", Integer.class)), () -> 0)
    .add(Bridge.of(typeOf("start", Integer.class), typeOf("decide", Integer.class)), a -> a+1)
    .add(PartingWay.of(typeOf("decide", Integer.class), typeOf("start", Integer.class), typeOf("end", Integer.class)), a -> a<3 ? Either.left(a) : Either.right(a))
    .add(End.of(typeOf("end", Integer.class)), i -> {
      values.add(i);
    })
    .build();

ProcessEngineLike pe = ProcessEngineLike.with(routes);

ProcessListener listener = ProcessListener.builder()
    .onStateChange((route, currentState) -> {
      if (currentState.type().name().equals("decide")) {
        values.add(currentState.value());
      }
    })
    .build();

pe.run(listener);

String dot = RoutesAsGraph.routeGraphAsDot("simpleLoop", RoutesAsGraph.asGraphIncludingStartAndEnd(routes.all()));
```

... and generate an dot file for this process enging graph: 

```
digraph simpleLoop {
	rankdir=LR;

	"start:class java.lang.Integer"[ shape="rectangle", label="start:Integer" ];
	"6d5d60c8-de5d-4aad-8537-38dccaf0b65e:class java.lang.Void"[ shape="circle", label="" ];
	"decide:class java.lang.Integer"[ shape="rectangle", label="decide:Integer" ];
	"end:class java.lang.Integer"[ shape="rectangle", label="end:Integer" ];
	"837b5448-9d4c-4cf1-ba20-bfd9c419ca78:class java.lang.Void"[ shape="circle", label="" ];

	"6d5d60c8-de5d-4aad-8537-38dccaf0b65e:class java.lang.Void" -> "start:class java.lang.Integer"[ label="Start" ];
	"start:class java.lang.Integer" -> "decide:class java.lang.Integer"[ label="Bridge" ];
	"decide:class java.lang.Integer" -> "start:class java.lang.Integer"[ label="PartingWay" ];
	"decide:class java.lang.Integer" -> "end:class java.lang.Integer"[ label="PartingWay" ];
	"end:class java.lang.Integer" -> "837b5448-9d4c-4cf1-ba20-bfd9c419ca78:class java.lang.Void"[ label="End" ];
}

```