# a generic way to build an init-like system

When you start an applications then there are some preconditions that must be fulfilled. 
And if this application should stop garbage has to be cleaned. This library provides building blocks to implement
such applications. 

## Building Blocks

An init-like system is more or less a graph of dependencies. So we define our system as transitions between vertices. 
A vertex is definied by a type and an optional name:

```java
${vertex}
```

Following transition types are possible:

```java
${transitions}
```

which can be created with a fluent api:

```java
${fluentTransitions}
```

The result of a transition must be wrapped into a `State`, which provides an optional tearDown hook:

```java
${state}
```

The tearDown is called if needed.

### Define a System

In the beginning you need to create something out of noting.

```java
${startTransitionWorks}
```

Our first dependency:

```java
${deriveShouldWork}
```

Merging two dependencies:

```java
${joinShouldWork}
```

Writing a custom transition:

```java
${customTransitionShouldWork}
```

The ordering of each entry does not matter. We only have to define our transitions, how to get to the destination is automatically resolved.
No transition is called twice and it is possible to work on an partial initialized system.

```java
${localInitShouldWork}
```

One way to join different independent transitions without collisions is to use a initialized
transition as a state which will be teared down automatically.

```java
${initAsStateShouldWork}
```

An other way is to wrap transitions and only expose incoming and outgoing connections.

```java
${wrappedTransitions}
```

... and generate an dot file for your application graph:

```
${wrappedTransitions.app.dot}
```


## Sample Application

... first we need a little helper:

[de.flapdoodle.java8 - Try](https://github.com/flapdoodle-oss/de.flapdoodle.java8/blob/master/src/main/java/de/flapdoodle/types/Try.java)

... create an temp directory

```java
${createATempDir}
```

... and create an file in this temp directory

```java
${createAFileInTempDir}
```

... write content into this file.

```java
${writeContentIntoFileInTempDir}
```

... and generate an dot file for your application graph: 

```
${writeContentIntoFileInTempDir.app.dot}
```

