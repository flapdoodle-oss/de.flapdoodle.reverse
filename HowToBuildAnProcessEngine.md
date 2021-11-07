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
Step<String, String> bridge;
Conditional<String, String, String> parting;
End<String> end;

start = Start.of(StateID.of(String.class),() -> "");
bridge = Step.of(StateID.of("a", String.class), StateID.of("b", String.class), it -> it);
parting = Conditional.of(StateID.of("start", String.class), StateID.of("oneDestination", String.class),
    StateID.of("otherDestination", String.class), it -> Either.left(it));
end = End.of(StateID.of("start", String.class), it -> {});
```

The result of a transition is wrapped into a `State` visible by a process listener:

```java
State<String> state = State.of(StateID.of("foo", String.class), "hello");
```

### Define a System

In the beginning you need to create something out of noting and end end wich resolves to nothing.

```java
  List<Edge> routes = Arrays.asList(
      Start.of(StateID.of(String.class), () -> "foo"),
      End.of(StateID.of(String.class), i -> {
          result.set(i);
      })
  );

ProcessEngine pe = ProcessEngine.with(routes);
ProcessEngine.Started started = pe.start();
do {
    states.add(started.currentState());
} while (started.next());

```

Transformation in between:

```java
  List<Edge> routes = Arrays.asList(
      Start.of(StateID.of(String.class), () -> "12"),
      Step.of(StateID.of(String.class), StateID.of(Integer.class), Integer::valueOf),
      End.of(StateID.of(Integer.class), i -> {
          result.set(i);
      })
  );

ProcessEngine pe = ProcessEngine.with(routes);
pe.start().forEach(state -> {
    // called for each new state
});
```

Simple looping process:

```java
  List<Edge> routes = Arrays.asList(
    Start.of(StateID.of("start", Integer.class), () -> 0),
    Step.of(StateID.of("start", Integer.class), StateID.of("decide", Integer.class), a -> a + 1),
    Conditional.of(StateID.of("decide", Integer.class), StateID.of("start", Integer.class),
        StateID.of("end", Integer.class), a -> a < 3 ? Either.left(a) : Either.right(a)),
    End.of(StateID.of("end", Integer.class), values::add)
    );

ProcessEngine pe = ProcessEngine.with(routes);
pe.start().forEach(currentState -> {
    if (currentState.type().name().equals("decide")) {
        values.add(currentState.value());
    }
});

String dot = RoutesAsGraph.routeGraphAsDot("simpleLoop", RoutesAsGraph.asGraphIncludingStartAndEnd(routes));
```

... and generate an dot file for this process enging graph: 

```
digraph simpleLoop {
	rankdir=LR;

	"start_1:class java.lang.Void"[ shape="circle", label="" ];
	"start:class java.lang.Integer"[ shape="rectangle", label="start:Integer" ];
	"decide:class java.lang.Integer"[ shape="rectangle", label="decide:Integer" ];
	"end:class java.lang.Integer"[ shape="rectangle", label="end:Integer" ];
	"end_2:class java.lang.Void"[ shape="circle", label="" ];

	"start_1:class java.lang.Void" -> "start:class java.lang.Integer"[ label="ImmutableStart" ];
	"start:class java.lang.Integer" -> "decide:class java.lang.Integer"[ label="ImmutableStep" ];
	"decide:class java.lang.Integer" -> "start:class java.lang.Integer"[ label="ImmutableConditional" ];
	"decide:class java.lang.Integer" -> "end:class java.lang.Integer"[ label="ImmutableConditional" ];
	"end:class java.lang.Integer" -> "end_2:class java.lang.Void"[ label="ImmutableEnd" ];
}

```