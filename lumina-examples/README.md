# Lumina examples

## Showcase (recommended)

`ShowcaseApp` is the recommended first run: an **interactive Streamlit-style demo**
(counter, progress, session state, layout, expander) — not a static dashboard.

```bash
mvn -q -pl lumina-examples -am install
mvn -q -pl lumina-examples exec:java -Dexec.mainClass=io.lumina.examples.showcase.ShowcaseMain
```

Open [http://localhost:8080](http://localhost:8080), click **Increment** or **Say hello**,
and watch the server rerun the Java `build()` method.

Install dependencies first (`-am install`) so sibling reactor artifacts are
available on a fresh checkout.

## Hello AI

Minimal stateful chat backed by the offline echo client. The module's default
`mainClass` is `HelloAiMain`, so no `-Dexec.mainClass` override is needed.

```bash
mvn -q -pl lumina-examples -am install
mvn -q -pl lumina-examples exec:java
```

## Streaming chat

`StreamingChatApp` shows `Ui.ai(TokenStream)`: it streams the offline echo
reply to the client token-by-token instead of writing the whole reply at
once, then persists the fully accumulated text in history.

```bash
mvn -q -pl lumina-examples -am install
mvn -q -pl lumina-examples exec:java -Dexec.mainClass=io.lumina.examples.streaming.StreamingChatMain
```

## Layout demo

`LayoutDemoApp` demonstrates nested layout primitives for integration tests:
sidebar, equal-width columns, container, and expander.

```bash
mvn -q -pl lumina-examples -am install
mvn -q -pl lumina-examples exec:java -Dexec.mainClass=io.lumina.examples.layout.LayoutDemoMain
```
