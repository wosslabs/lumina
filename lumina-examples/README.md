# Lumina examples

Runnable samples for all three integration paths. Examples are **not** published to Maven
Central (`maven.deploy.skip`).

| Path | Module | Stack | UI URL |
|------|--------|-------|--------|
| **A — Standalone** | [`lumina-examples`](.) | `lumina-web` + `LuminaServer.start` | [http://127.0.0.1:8080](http://127.0.0.1:8080) |
| **B — Spring Boot** | [`lumina-examples-spring`](../lumina-examples-spring) | starter + WebMVC | [http://127.0.0.1:8090](http://127.0.0.1:8090) |
| **C — Boot + Spring AI** | [`lumina-examples-spring-ai`](../lumina-examples-spring-ai) | starter + Spring AI (echo if no key) | [http://127.0.0.1:8090](http://127.0.0.1:8090) |

Always unset Cursor’s injected Spring import when running Boot examples:

```bash
env -u SPRING_CONFIG_IMPORT mvn …
```

Install reactor artifacts first on a fresh checkout: `mvn -q -am install` (or `-pl <module> -am`).

---

## Path A — Standalone (`lumina-examples`)

### Showcase (recommended)

`ShowcaseApp` is the recommended first run: interactive Streamlit-style demo **plus**
complete AI cockpit pages — **RAG chat**, **Agent workbench**, and **MCP tool console**.

```bash
mvn -q -pl lumina-examples -am install
mvn -q -pl lumina-examples exec:java -Dexec.mainClass=io.lumina.examples.showcase.ShowcaseMain
```

Open [http://127.0.0.1:8080](http://127.0.0.1:8080). Sidebar routes: `/`, `/widgets`, `/ai`,
`/rag`, `/agent`, `/mcp`, `/about`.

See also [docs/AI_GUIDE.md](../docs/AI_GUIDE.md).

### Dedicated AI demos

```bash
mvn -q -pl lumina-examples exec:java -Dexec.mainClass=io.lumina.examples.ai.RagChatMain
mvn -q -pl lumina-examples exec:java -Dexec.mainClass=io.lumina.examples.ai.AgentWorkbenchMain
mvn -q -pl lumina-examples exec:java -Dexec.mainClass=io.lumina.examples.ai.McpConsoleMain
```

### Hello AI

Minimal stateful chat backed by the offline echo client. The module's default
`mainClass` is `HelloAiMain`, so no `-Dexec.mainClass` override is needed.

```bash
mvn -q -pl lumina-examples -am install
mvn -q -pl lumina-examples exec:java
```

### Streaming chat

`StreamingChatApp` shows `Ui.ai(TokenStream)`: it streams the offline echo
reply to the client token-by-token instead of writing the whole reply at
once, then persists the fully accumulated text in history.

```bash
mvn -q -pl lumina-examples -am install
mvn -q -pl lumina-examples exec:java -Dexec.mainClass=io.lumina.examples.streaming.StreamingChatMain
```

### Layout demo

`LayoutDemoApp` demonstrates nested layout primitives for integration tests:
sidebar, equal-width columns, container, and expander.

```bash
mvn -q -pl lumina-examples -am install
mvn -q -pl lumina-examples exec:java -Dexec.mainClass=io.lumina.examples.layout.LayoutDemoMain
```

---

## Path B — Spring Boot

See [`lumina-examples-spring/README.md`](../lumina-examples-spring/README.md).

```bash
env -u SPRING_CONFIG_IMPORT mvn -q -pl lumina-examples-spring -am spring-boot:run
```

Open **[http://127.0.0.1:8090/](http://127.0.0.1:8090/)** (Lumina Jetty). Tomcat on `:8080` often
shows **Disconnected** — that is expected when Spring Web is on the classpath.

---

## Path C — Spring Boot + Spring AI

See [`lumina-examples-spring-ai/README.md`](../lumina-examples-spring-ai/README.md).

```bash
env -u SPRING_CONFIG_IMPORT mvn -q -pl lumina-examples-spring-ai -am spring-boot:run
# optional live model:
# OPENAI_API_KEY=… env -u SPRING_CONFIG_IMPORT mvn -q -pl lumina-examples-spring-ai -am spring-boot:run
```

Without a key the app uses offline echo. With `OPENAI_API_KEY`, Spring AI wires a live
`ChatModel` and Lumina streams through `SpringAiChatClient`. UI:
[http://127.0.0.1:8090/](http://127.0.0.1:8090/).
