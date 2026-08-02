# Lumina AI Guide

How to build **complete AI UIs** with Lumina — RAG, agents, and MCP-style tool consoles —
while keeping orchestration in Spring AI / MCP clients / your agent framework.

!!! tip "Mental model"
    **Lumina = cockpit (UI).**  
    **Spring AI / MCP / agents = engines (orchestration).**

Lumina does not embed a vector database, MCP protocol stack, or multi-agent runtime.
It gives you the widgets and session model to ship those products quickly on the JVM.

---

## Quick demos (run locally)

```bash
mvn -q -pl lumina-examples -am install

# All-in-one showcase (sidebar: RAG / Agent / MCP)
mvn -q -pl lumina-examples exec:java -Dexec.mainClass=io.lumina.examples.showcase.ShowcaseMain

# Or dedicated apps
mvn -q -pl lumina-examples exec:java -Dexec.mainClass=io.lumina.examples.ai.RagChatMain
mvn -q -pl lumina-examples exec:java -Dexec.mainClass=io.lumina.examples.ai.AgentWorkbenchMain
mvn -q -pl lumina-examples exec:java -Dexec.mainClass=io.lumina.examples.ai.McpConsoleMain
```

Open [http://127.0.0.1:8080/](http://127.0.0.1:8080/).

---

## What Lumina ships for AI

| Widget / API | Use for |
|--------------|---------|
| `ui.chatInput()` / `ui.user` / `ui.ai` | Chat transcript |
| `ui.ai(TokenStream)` | Token streaming |
| `ui.citation` / `ui.ragSources` | RAG evidence |
| `ui.toolCall` / `ui.toolInvocation` | Tool / MCP call rows |
| `ui.usage` | Tokens / cost / latency |
| `ui.agentTimeline` | Multi-step agent progress |
| `ui.approval` | Human-in-the-loop gate |
| `ui.memoryPanel` | Session / long-term memory view |
| `SpringAiChatClient` | Bridge Spring AI → `TokenStream` |

---

## Pattern 1 — RAG chat

1. Retrieve chunks (your code / Spring AI retriever).
2. Render `ui.ragSources(...)` + optional `ui.citation(...)`.
3. Augment the prompt with chunk text.
4. Stream with `ui.ai(provider.stream(augmented))`.
5. Persist turns in `ui.state()`.

Demo implementation: `io.lumina.examples.ai.RagChatPages` (in-memory keyword corpus).

```java
List<Map<String, Object>> sources = retrieve(prompt); // your vector store
ui.ragSources(sources);
String reply = ui.ai(springAi.stream(augment(prompt, sources)));
```

---

## Pattern 2 — Tool-calling agent with approval

1. Show `ui.agentTimeline` steps (plan → tools → approve → answer).
2. Execute tools; show `ui.toolInvocation` / `ui.toolCall`.
3. Gate publication with `ui.approval("…")`.
4. On approval, render `ui.ai(draft)` and update `ui.memoryPanel`.

Demo: `io.lumina.examples.ai.AgentWorkbenchPages`.

This is the UI shape for Spring AI tool-calling agents, LangChain4j agents, or a hand-rolled loop.

---

## Pattern 3 — MCP tool console

1. List tools from your MCP client → `ui.table(...)`.
2. Let the user pick a tool + arguments.
3. Call MCP; render `ui.toolCall` / `ui.toolInvocation` / `ui.json`.
4. Keep a call history table in session state.

Demo: `io.lumina.examples.ai.McpConsolePages` + `DemoMcpCatalog` (simulated tools).

Lumina 1.0 does **not** ship an MCP SDK. Use any Java MCP client and keep Lumina as the console.

---

## Spring AI streaming (production LLM)

```xml
<dependency>
  <groupId>io.github.wosslabs</groupId>
  <artifactId>lumina-spring-ai</artifactId>
  <version>1.0.0</version>
</dependency>
```

```java
@Bean
LuminaApp app(org.springframework.ai.chat.client.ChatClient springAi) {
  var chat = new io.lumina.springai.SpringAiChatClient(springAi);
  return ui -> {
    String prompt = ui.chatInput();
    if (prompt != null) {
      ui.user(prompt);
      ui.ai(chat.stream(prompt));
    }
  };
}
```

Keep keys in env vars (`OPENAI_API_KEY`, etc.). See [DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md) Path C.

---

## Responsibility split

| Concern | Owner |
|---------|--------|
| Buttons, layout, streaming bubbles, citations, approvals | **Lumina** |
| Embeddings, vector DB, chunking | Your app / Spring AI |
| MCP JSON-RPC, tool schemas, servers | MCP client/server SDK |
| Multi-agent graphs, planners | Agent framework |
| AuthN for tools / tenants | Your security layer + Lumina role hooks |

---

## Checklist: “AI feels complete”

- [ ] Chat history replayed from `ui.state()` every run
- [ ] Streaming via `TokenStream` (not only blocking `prompt`)
- [ ] RAG answers always show `ragSources` / `citation`
- [ ] Tool/MCP calls visible with input+output
- [ ] Risky actions gated with `approval`
- [ ] Usage metrics shown for cost/latency awareness
- [ ] Offline echo path for local demos without keys

When those are true, juniors can ship credible AI UIs on day one — and swap the engine later without rewriting the cockpit.
