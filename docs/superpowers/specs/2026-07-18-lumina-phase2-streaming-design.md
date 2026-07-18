# Lumina Phase 2 — AI-Native Streaming + Spring AI Adapter

**Status:** Approved (design dialogue 2026-07-18)
**Depends on:** Phase 1 MVP (`docs/superpowers/specs/2026-07-18-lumina-phase1-design.md`)
**Version target:** `0.2.0-SNAPSHOT`
**Java:** 21+

## 1. Goal

Make Lumina genuinely AI-native by streaming model output token-by-token to the browser, and ship a real LLM integration via Spring AI — without pulling any provider or reactive dependency into `lumina-core`.

### Canonical example (streaming, still &lt;20 lines of user code)

```java
public final class StreamingChatApp implements LuminaApp {
    private final ChatClient chat = ChatClients.echo(); // or a Spring AI-backed bean

    @Override
    public void build(Ui ui) {
        ui.title("Streaming Chat");
        List<String[]> history = ui.state().computeIfAbsent("history", k -> new ArrayList<>());
        for (String[] turn : history) {
            ui.user(turn[0]);
            ui.ai(turn[1]);
        }
        String prompt = ui.chatInput();
        if (prompt != null) {
            ui.user(prompt);
            String reply = ui.ai(chat.stream(prompt)); // streams tokens live, returns full text
            history.add(new String[] { prompt, reply });
        }
    }
}
```

`ui.ai(TokenStream)` streams chunks to the browser live and returns the fully accumulated
text once the stream completes, so the app can persist it to history (see §4.4).

## 2. Decisions locked in design dialogue

| Topic | Choice |
|-------|--------|
| API shape | `ui.ai(TokenStream)` overload; `ChatClient.stream(prompt)` returns a token source |
| Token type | Framework-owned `TokenStream` (blocking `Iterable<String>`); reactive bridging lives in the adapter |
| Runtime model | Stream synchronously inside `build()` on the session virtual thread; live frames via a `FrameSink` |
| Cancellation | Deferred (no stop/cancel in Phase 2) |
| Real adapter | Spring AI adapter in a new `lumina-spring-ai` module |
| Protocol | Add a `stream` message type (start/append/end); snapshot/patch/error unchanged |

## 3. Public API (`lumina-core`)

All additions preserve Phase 1 binary compatibility (new types + `default` methods only).

```java
package io.lumina.ai;

/**
 * A blocking, forward-only source of text chunks produced by a {@link ChatClient}.
 * Iteration blocks until the next chunk is available or the stream ends.
 */
public interface TokenStream extends Iterable<String> {
    /** Wraps a complete string as a single-chunk stream (non-streaming clients). */
    static TokenStream of(String whole);

    /** Wraps an existing iterator/iterable of chunks. */
    static TokenStream fromIterable(Iterable<String> chunks);
}
```

```java
package io.lumina.ai;

public interface ChatClient {
    String prompt(String input);                       // unchanged (Phase 1)

    /**
     * Streams the completion as chunks. Default yields the whole {@link #prompt} result
     * as one chunk, so existing non-streaming clients remain source/binary compatible.
     */
    default TokenStream stream(String input) {
        return TokenStream.of(prompt(input));
    }
}
```

```java
package io.lumina.ui;

public interface Ui {
    // ... Phase 1 methods unchanged ...

    /**
     * Renders an assistant chat message whose text streams in chunk-by-chunk. Blocks until
     * the stream completes and returns the fully accumulated text (for history persistence).
     * The accumulated text also becomes the node's content in the component tree.
     */
    String ai(TokenStream tokens);
}
```

`ChatClients.echo()` returns a client whose `stream()` chunks the echoed text word-by-word (small inter-chunk delay optional and off by default) so streaming works offline in demos and tests.

## 4. Runtime streaming model (`lumina-runtime`)

### 4.1 Execution
- `ui.ai(TokenStream)` executes during `build()` on the session's serial virtual thread.
- The `UiBinder`:
  1. allocates the `ai_message` node with its deterministic key/id (Phase 1 keying) and empty content;
  2. emits a live `stream start` frame for that id via the `FrameSink`;
  3. iterates the `TokenStream`, appending each chunk to the node's accumulated content and emitting a live `stream append` frame per chunk (optionally coalesced, see §4.3);
  4. emits a live `stream end` frame;
  5. leaves the node in the tree with the **full** accumulated text.
- Serial-per-session semantics are unchanged: the session thread is busy for the duration of the stream; other intents queue (consistent with Phase 1).

### 4.2 Frame sink wiring
- `SessionHandle.submit(Intent intent, FrameSink sink)` is added. The existing `submit(Intent)` remains and delegates with a no-op sink (binary compatible).
- `FrameSink` (runtime SPI):

```java
package io.lumina.runtime;

/** Receives live frames produced during a single run (e.g. streaming tokens). */
@FunctionalInterface
public interface FrameSink {
    void send(String json);
    FrameSink NOOP = json -> { };
}
```

- `AppRunner.run(app, session, intent, sink)` passes the sink into the `UiBinder`.

### 4.3 Suppression + coalescing
- **Suppression:** because streamed subtrees are already materialized on the client via live frames, the final `RunResult` patch list omits ADD/UPDATE ops for streamed node subtrees produced *this run*. `previousRoot` still stores the complete new tree, so the next rerun diffs correctly.
- **Coalescing (optional, configurable):** append frames may be coalesced by a small time window (default 0 ms = per chunk in Phase 2). The knob exists but defaults to immediate emission.

### 4.4 History persistence
`ui.ai(TokenStream)` returns the fully accumulated text once the stream completes, so the app
can persist it (e.g. `history.add(new String[]{prompt, reply})`) exactly as the Phase 1 example
persisted the non-streaming reply. The `UiBinder` accumulates chunks as it emits live frames and
returns the joined result; no separate collection API is needed.

## 5. Wire protocol (ADR-006)

Add one message type; snapshot/patch/error/intent are unchanged.

**Server → Client**

```json
{ "type": "stream", "id": "<aiNodeId>", "op": "start" }
{ "type": "stream", "id": "<aiNodeId>", "op": "append", "text": "Hel" }
{ "type": "stream", "id": "<aiNodeId>", "op": "end" }
```

- `start`: client ensures the target `ai_message` element exists (created by the preceding snapshot/patch ADD) and shows a typing indicator.
- `append`: client appends `text` to the element via `textContent` (XSS-safe, no `innerHTML`).
- `end`: client clears the typing indicator; content is final.

Ordering guarantee: the ADD for the `ai_message` node is sent (as a normal patch/snapshot) before its `stream start`, so the client always has the target element. On reconnect, a fresh snapshot carries the full content (no replay of stream frames needed).

## 6. Spring AI adapter (`lumina-spring-ai`)

- New optional module: depends on `lumina-core` + Spring AI; **`lumina-core`/`lumina-runtime` remain provider- and reactive-free.**
- `SpringAiChatClient implements ChatClient`:
  - `prompt(input)` → blocking Spring AI call.
  - `stream(input)` → bridges Spring AI's reactive `Flux<String>` to a blocking `TokenStream` by draining into a bounded `BlockingQueue<String>` with an end/error sentinel; the consuming session virtual thread blocks cheaply.
- Auto-configuration (in this module or the existing starter): registers a `ChatClient` bean backed by Spring AI when a Spring AI `ChatModel`/`ChatClient` is present and configured; otherwise the app keeps whatever `ChatClient` it constructs (e.g. echo). No provider credentials are hardcoded — all via Spring AI's standard properties/environment.

## 7. Module changes

| Module | Change |
|--------|--------|
| `lumina-core` | Add `TokenStream`; add `ChatClient.stream` default; add `Ui.ai(TokenStream)`; streaming `echo` |
| `lumina-runtime` | Add `FrameSink`; streaming in `UiBinder`; `AppRunner`/`SessionHandle` sink overloads; suppression |
| `lumina-web` | `LuminaWebSocketEndpoint` passes a `FrameSink` that writes frames to the WS session; `ProtocolCodec` serializes `stream` frames |
| `lumina-web` (client) | `lumina-client.js` handles `stream` start/append/end |
| `lumina-spring-ai` (new) | `SpringAiChatClient` + Flux→TokenStream bridge + optional auto-config |
| `lumina-examples` | `StreamingChatApp` + main |
| root `pom.xml` | Register `lumina-spring-ai`; add Spring AI BOM; bump version to `0.2.0-SNAPSHOT` |

## 8. Testing strategy

| Layer | Test |
|-------|------|
| `TokenStream.of` / `fromIterable` / echo streaming | JUnit unit |
| `ChatClient.stream` default behavior | JUnit unit |
| `UiBinder` streaming: node id, live frames, accumulated content, suppression | Runtime unit |
| `AppRunner` streaming run with a `FrameSink` capture | Runtime unit |
| WebSocket `stream` frame sequence (start→append*→end) | `lumina-web` IT (JDK WS client) |
| Spring AI `Flux<String>` → `TokenStream` bridge | Unit (mocked Flux, e.g. `Flux.just(...)`) |
| `StreamingChatApp` echo streaming end-to-end | Example smoke IT asserting appended `Echo:` chunks |

## 9. Compatibility & versioning

- Bump to `0.2.0-SNAPSHOT`; MINOR bump (additive, backward compatible).
- All Phase 1 public APIs unchanged; new APIs are additive; `ChatClient.stream` and `submit(Intent)` use defaults/overloads to preserve binary compatibility.
- ADR-006 documents the streaming protocol.

## 10. Non-goals (deferred)

- Stop/cancel of an in-flight stream (needs an out-of-band control channel).
- Tool/function calling, multi-modal, structured output.
- Non-Spring provider adapters (e.g. direct OpenAI HTTP).
- SSE transport (Phase 3), reconnect/resume beyond fresh snapshot.
- Backpressure tuning beyond a bounded queue + optional coalescing window.

## 11. ADRs

| ID | Title |
|----|-------|
| ADR-006 | Streaming token protocol and runtime frame sink |

## 12. Deliverables checklist

- [ ] `TokenStream` + streaming `echo` + `ChatClient.stream` default
- [ ] `Ui.ai(TokenStream)` returning accumulated text
- [ ] `FrameSink` + runtime streaming + suppression
- [ ] `stream` frames in `ProtocolCodec` + WebSocket endpoint
- [ ] Client `stream` handling in `lumina-client.js`
- [ ] `lumina-spring-ai` module: `SpringAiChatClient` + Flux bridge + optional auto-config
- [ ] `StreamingChatApp` example (&lt;20 lines) + main
- [ ] Unit + integration tests (§8)
- [ ] ADR-006
- [ ] Version bump to `0.2.0-SNAPSHOT`
- [ ] Javadoc on all new public APIs

## 13. Next step

After approval, produce a detailed implementation plan (`docs/superpowers/plans/…`) via the writing-plans skill, then execute with subagent-driven development.
