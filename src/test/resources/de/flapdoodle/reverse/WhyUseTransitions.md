# Why use Transitions

If you have to test some IO related problems you may find yourself in an situation where you have to create some
temporary files, and if you dont want to leave the garbage behind, to delete everything after the test is done:

```java
${startProblem}
```

.. since java 7 you can wrap some if this into an Closable so that you can use the try-with-resources pattern:

```java
${changeToTryWithResources}
```

As this example is not very complicated, even this looks a little bit like over-engineered.
But we can even go further (don't be afraid, in the end i hope you understand why we are doing this).

```java
${useTransitions}
```

TODO
* special classes
* bundle patterns (interfaces with default methods, builder-pattern, ...)

