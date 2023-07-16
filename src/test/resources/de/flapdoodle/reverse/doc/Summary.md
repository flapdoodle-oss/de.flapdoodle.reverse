# How do we get here?
                     
Maybe you hat to write the following code:

```java
${intro}
```


As you did not create a matching template with the name

> `Summary.md`

in the same package as

> `de.flapdoodle.reverse.doc.SummaryTest`

the content of this file is generated from the recordings of your test class.

In your test following parts were recorded:

* `intro`
* `intro.1`
* `startTransition`
* `startTransition.1`
* `tryWithResources`
* `tryWithResources.1`
* `transitions`
* `transitions.1`
* `abstractFileCreation`
* `abstractFileCreation.1`
* `Hashing.java`
* `FileFactory.java`
* `transitions.transitions.dot`

To insert the content of a part into the generated document you must embed a name
from this list between a starting `${` and `}`.