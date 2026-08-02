# Lumina examples

## Showcase (recommended)

`ShowcaseApp` is the recommended first run: interactive Streamlit-style demo **plus**
complete AI cockpit pages — **RAG chat**, **Agent workbench**, and **MCP tool console**.

```bash
mvn -q -pl lumina-examples -am install
mvn -q -pl lumina-examples exec:java -Dexec.mainClass=io.lumina.examples.showcase.ShowcaseMain
```

Open [http://127.0.0.1:8080](http://127.0.0.1:8080). Sidebar routes: `/`, `/widgets`, `/ai`,
`/rag`, `/agent`, `/mcp`, `/about`.

See also [docs/AI_GUIDE.md](../docs/AI_GUIDE.md).

Install dependencies first (`-am install`) so sibling reactor artifacts are
available on a fresh checkout.

## Dedicated AI demos

```bash
mvn -q -pl lumina-examples exec:java -Dexec.mainClass=io.lumina.examples.ai.RagChatMain
mvn -q -pl lumina-examples exec:java -Dexec.mainClass=io.lumina.examples.ai.AgentWorkbenchMain
mvn -q -pl lumina-examples exec:java -Dexec.mainClass=io.lumina.examples.ai.McpConsoleMain
```

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
