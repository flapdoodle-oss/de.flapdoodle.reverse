# a generic way to build an process engine

This library provides building blocks to implement a simple process engine. 

## Building Blocks

An process engine is more or less a graph of dependencies. So we define our system as transitions between vertices. 
A vertex is definied by a type and an optional name:

```java
StateID<String> id = StateID.of(String.class);
StateID<String> idWithLabel = StateID.of("foo", String.class);
```

Following transition types are possible:

```java
Start<String> start;
Bridge<String, String> bridge;
PartingWay<String, String, String> parting;
End<String> end;

start = Start.of(StateID.of(String.class));
bridge = Bridge.of(StateID.of("a", String.class), StateID.of("b", String.class));
parting = PartingWay.of(StateID.of("start", String.class), StateID.of("oneDestination", String.class),
    StateID.of("otherDestination", String.class));
end = End.of(StateID.of("start", String.class));
```

The result of a transition is wrapped into a `State` visible by a process listener:

```java
State<String> state = State.of(StateID.of("foo", String.class), "hello");
```

You can listen to events with an ProcessListener:

```java
ProcessListener listener = ProcessListener.builder()
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
ProcessRoutes<SingleSource<?, ?>> routes = ProcessRoutes.builder()
    .add(Start.of(StateID.of(String.class)), () -> "foo")
    .add(End.of(StateID.of(String.class)), i -> {
      result.set(i);
    })
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
ProcessRoutes<SingleSource<?, ?>> routes = ProcessRoutes.builder()
    .add(Start.of(StateID.of(String.class)), () -> "12")
    .add(Bridge.of(StateID.of(String.class), StateID.of(Integer.class)), a -> Integer.valueOf(a))
    .add(End.of(StateID.of(Integer.class)), i -> {
      result.set(i);
    })
    .build();

ProcessEngineLike pe = ProcessEngineLike.with(routes);

pe.run(ProcessListener.noop());
```

Simple looping process:

```java
ProcessRoutes<SingleSource<?, ?>> routes = ProcessRoutes.builder()
    .add(Start.of(StateID.of("start", Integer.class)), () -> 0)
    .add(Bridge.of(StateID.of("start", Integer.class), StateID.of("decide", Integer.class)), a -> a + 1)
    .add(PartingWay.of(StateID.of("decide", Integer.class), StateID.of("start", Integer.class),
        StateID.of("end", Integer.class)), a -> a < 3 ? Either.left(a) : Either.right(a))
    .add(End.of(StateID.of("end", Integer.class)), i -> {
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
	"start_1:class java.lang.Void"[ shape="circle", label="" ];
	"decide:class java.lang.Integer"[ shape="rectangle", label="decide:Integer" ];
	"end:class java.lang.Integer"[ shape="rectangle", label="end:Integer" ];
	"end_2:class java.lang.Void"[ shape="circle", label="" ];

	"start_1:class java.lang.Void" -> "start:class java.lang.Integer"[ label="Start" ];
	"start:class java.lang.Integer" -> "decide:class java.lang.Integer"[ label="Bridge" ];
	"decide:class java.lang.Integer" -> "start:class java.lang.Integer"[ label="PartingWay" ];
	"decide:class java.lang.Integer" -> "end:class java.lang.Integer"[ label="PartingWay" ];
	"end:class java.lang.Integer" -> "end_2:class java.lang.Void"[ label="End" ];
}

```