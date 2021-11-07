# a generic way to build an process engine

This library provides building blocks to implement a simple process engine. 

## Building Blocks

An process engine is more or less a graph of dependencies. So we define our system as transitions between vertices. 
A vertex is definied by a type and an optional name:

```java
${vertex}
```

Following transition types are possible:

```java
${edges}
```

The result of a transition is wrapped into a `State` visible by a process listener:

```java
${state}
```

### Define a System

In the beginning you need to create something out of noting and end end wich resolves to nothing.

```java
${startAndEnd}
```

Transformation in between:

```java
${startBridgeAndEnd}
```

Simple looping process:

```java
${loopSample}
```

... and generate an dot file for this process enging graph: 

```
${loopSample.dotFile}
```
