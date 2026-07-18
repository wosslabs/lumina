# Lumina examples

Run Hello AI from the repository root:

```bash
mvn -q -pl lumina-examples -am install
mvn -q -pl lumina-examples exec:java
```

The `exec-maven-plugin` in this module sets `mainClass` to
`io.lumina.examples.helloai.HelloAiMain`, so the second command does not
need `-Dexec.mainClass`. Install dependencies first (`-am install`) so
sibling SNAPSHOT artifacts are available on a fresh checkout; a single-line
`… install exec:java` runs `exec:java` on the parent aggregator as well and
fails without a `mainClass` there.
