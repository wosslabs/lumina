# Lumina examples

## Showcase (recommended)

`ShowcaseApp` is the hero demo for P1.5 UX: wide layout, sidebar rail, page title,
and styled widgets (columns, progress, expander, text input, button).

```bash
mvn -q -pl lumina-examples -am install
mvn -q -pl lumina-examples exec:java -Dexec.mainClass=io.lumina.examples.showcase.ShowcaseMain
```

Open the printed URL (default [http://localhost:8080](http://localhost:8080)) to
see the app shell and design system in light or dark mode.

Install dependencies first (`-am install`) so sibling SNAPSHOT artifacts are
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
