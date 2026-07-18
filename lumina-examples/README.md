# Lumina examples

Run Hello AI from the repository root:

```bash
mvn -q -pl lumina-examples -am package
mvn -q -pl lumina-examples exec:java
```

The `exec-maven-plugin` in this module sets `mainClass` to
`io.lumina.examples.helloai.HelloAiMain`, so the second command does not
need `-Dexec.mainClass`. Build dependencies first (`-am package`); a
single-line `… package exec:java` runs `exec:java` on the parent
aggregator as well and fails without a `mainClass` there.
