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

## Streaming chat

`StreamingChatApp` shows `Ui.ai(TokenStream)`: it streams the offline echo
reply to the client token-by-token instead of writing the whole reply at
once, then persists the fully accumulated text in history.

```bash
mvn -q -pl lumina-examples -am install
mvn -q -pl lumina-examples exec:java -Dexec.mainClass=io.lumina.examples.streaming.StreamingChatMain
```

Since this module's default `mainClass` is `HelloAiMain`, running the
streaming example overrides it with `-Dexec.mainClass`.
