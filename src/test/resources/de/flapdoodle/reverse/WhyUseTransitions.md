# Why you might use this library.

If you have to test some IO related problems you may find yourself in a situation where you have to create some
temporary files, and if you don't want to leave the garbage behind, to delete everything after the test is done.

As an example we are trying to create a directory, write some file to it, copy the content to a new file.
And after that we delete everything we created:

```java
${startProblem}
```

(the asserts are there to express some expectations:)

.. since java 7 you can wrap some of this into an `AutoClosable` so that you can use the try-with-resources pattern.
We can create a very special wrapper:

```java
${autoCleanUp.1}
```

.. and rewrite everything using try-with-resources, so that after the block is executed, the close method is called:

```java
${autoCleanUp.2}
```

As this example is not very complicated, even this looks a little over-engineered.
But life is more complicated than that. So let's have a look how we can solve this in another way.

First we need some unique identifier. It's like a variable name with some type information:

```java
${useTransitions.stateIds}
```

Next we describe how the values are created. As we already have the value for 'tempDir', we can just use this value
and map it to our "variable name". If we need some other values we declare them as given. As we want to remove
everything after it's usage, we can provide a callback with this value:

```java
${useTransitions.transitions}
```

As we have all descriptions, we can work with them. The idea behind this all is as following. As each description
contains the information for what to do, what is needed and how we name the result, we can create a graph from
this information. In this way we know where to start and what is need is we want to get the next result. If something
is missing, or we have some kind of loop, it will fail. Because we don't want to deal with this graph by ourselves,
we use 'Transitions' as a wrapper around this kind of stuff:  

```java
${useTransitions.graph}
```

In this example we try to emulate the code from the previous sample (checks included):

```java
${useTransitions.usage}
```

But we don't have to do it this way. We can, but we must not initialize every state. But if we are only interested
in some or one part of it, everything else is working as expected. While this is running inside the same test method
we can reuse the transition instance and just run this again. This time we are asking for only one state:

```java
${useTransitions.second-usage}
```

As we can build a graph we can also render this graph. Here we can render it as [Dot File](https://graphviz.org/doc/info/lang.html):                                                              

```java
${useTransitions.export-dot}
```

This is the [Dot-File](https://graphviz.org/doc/info/lang.html) for this example:

```dot
${useTransitions.copy-file.dot}
```

![Example-Dot](WhyUseTransitions.png)
