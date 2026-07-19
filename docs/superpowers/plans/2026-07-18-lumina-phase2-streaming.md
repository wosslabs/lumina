# Lumina Phase 2 Implementation Plan — AI Streaming + Spring AI

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stream LLM output token-by-token to the browser via `ui.ai(TokenStream)`, ship a Spring AI adapter, and keep `lumina-core` provider- and reactive-free.

**Architecture:** `ui.ai(TokenStream)` runs inside `build()` on the session's virtual thread. At that call the runtime flushes an interim `patch` (creating the user + empty ai_message nodes through the normal diff path), then streams text-only `stream` frames (`start`/`append`/`end`) to the existing element, then finishes the run with a suppressed redundant update. A new `lumina-spring-ai` module bridges Spring AI's reactive `Flux<String>` to the blocking `TokenStream`.

**Tech Stack:** Java 21, Maven, JUnit 5, AssertJ, Jetty 12 WebSocket, Jackson 2.18, Spring AI 1.0.x (adapter module only).

## Global Constraints

- Version: bump reactor to `0.2.0-SNAPSHOT` (MINOR, additive, backward compatible).
- `lumina-core` and `lumina-runtime` MUST NOT depend on Spring, Spring AI, Jetty, Servlet, or any reactive-streams library.
- Public API additions only; preserve Phase 1 binary compatibility via new types, `default` methods, and overloads (never change existing signatures).
- Streaming wire additions: only a new `stream` message type; `snapshot`/`patch`/`error`/`intent` unchanged.
- Client stays dependency-free and XSS-safe: append text via `textContent`, never `innerHTML`.
- No hardcoded provider credentials — Spring AI config via standard Spring properties/environment only.
- Spec: `docs/superpowers/specs/2026-07-18-lumina-phase2-streaming-design.md`; ADR-006 to be authored in this plan.
- Verify with `mvn -q clean test` after logic changes; commit small, cohesive units per task.

## Interfaces recap (Phase 1 code this plan builds on)

- `UiBinder implements Ui` (flat `children` list under `root`; `ai(String)` adds `ai_message` node with prop `content`; keys via `nextKey(type)` → `auto:/<type>#<index>`).
- `AppRunner.run(LuminaApp, SessionState, Intent) : RunResult` (package-private); retains `previousRoot`, uses `TreeDiffer`.
- `SessionHandle.submit(Intent) : CompletableFuture<RunResult>` → `SessionExecutor.submit(Supplier<T>)`.
- `RunResult(root, patches, fullSnapshot, error)`; factories `snapshot`, `patched`, `error`.
- `ProtocolCodec` (Jackson records `SnapshotMessage`/`PatchMessage`/`ErrorMessage`/`IntentMessage`); `toSnapshotJson`, `toPatchJson`, `toErrorJson`, `parseIntent`.
- `LuminaWebSocketEndpoint.onOpen`/`onMessage` call `sessionHandle.submit(...).whenComplete(reply)`; `reply` sends one frame.
- `lumina-client.js`: `LuminaApp` element with `applyPatch(ops)`, `renderNode(node)`; `ELEMENTS` map includes `ai_message: "lumina-ai-message"`; `LuminaAiMessage.content` reads `props.content`.

---

### Task 1: Version bump + ADR-006

**Files:**
- Modify: root `pom.xml` and every module `pom.xml` `<version>`/parent `<version>` `0.1.0-SNAPSHOT` → `0.2.0-SNAPSHOT`
- Create: `docs/adr/ADR-006-streaming-protocol.md`

**Interfaces:**
- Consumes: Phase 1 reactor
- Produces: `0.2.0-SNAPSHOT` reactor; ADR-006 documenting the streaming mechanism and `stream` frames

- [ ] **Step 1: Bump versions**

Change `0.1.0-SNAPSHOT` → `0.2.0-SNAPSHOT` in the root POM `<version>` and in each module POM's `<parent><version>` (and any managed `io.lumina` dependency versions using a literal). Use a single find/replace across `pom.xml` files; verify none remain:

Run: `grep -rl "0.1.0-SNAPSHOT" --include=pom.xml .`
Expected: no output.

- [ ] **Step 2: Write ADR-006**

Create `docs/adr/ADR-006-streaming-protocol.md`:

```markdown
# ADR-006: Streaming token protocol and runtime frame sink

## Status
Accepted

## Context
Phase 2 streams LLM output token-by-token. The client is a tree-diff renderer;
mid-build partial updates must reuse the existing patch path so the client only
learns to append text, not to create elements from a side channel.

## Decision
- `ui.ai(TokenStream)` runs inside build() on the session virtual thread.
- A runtime RunSink delivers interim structural patches and text frames for the current run.
- At the ui.ai(TokenStream) call the runtime FLUSHES an interim patch (diff of
  children-so-far vs the last delivered baseline), creating the user + empty
  ai_message nodes through the normal patch path and advancing the baseline.
- Then it emits text-only frames: {type:"stream", id, op:"start"},
  {type:"stream", id, op:"append", text}, {type:"stream", id, op:"end"}.
- The ai_message node ends the run with full accumulated content. The final diff
  suppresses UPDATE_PROPS for streamed node ids (client already has the text).
- Frame ordering for a streamed node: flush(patch) -> start -> append* -> end.
- On reconnect a fresh snapshot carries full content; stream frames are not replayed.

## Consequences
Serial-per-session semantics unchanged; client change is minimal (append text by
node id). Redundant final update avoided via suppression. Cancellation deferred.
```

- [ ] **Step 3: Verify + commit**

Run: `mvn -q clean install -DskipTests`
Expected: BUILD SUCCESS.

```bash
git add pom.xml lumina-*/pom.xml docs/adr/ADR-006-streaming-protocol.md
git commit -m "$(cat <<'EOF'
build: bump to 0.2.0-SNAPSHOT and add ADR-006 streaming protocol
EOF
)"
```

---

### Task 2: TokenStream + streaming echo + ChatClient.stream (core)

**Files:**
- Create: `lumina-core/src/main/java/io/lumina/ai/TokenStream.java`
- Modify: `lumina-core/src/main/java/io/lumina/ai/ChatClient.java` (add `default stream`)
- Modify: `lumina-core/src/main/java/io/lumina/ai/EchoChatClient.java` (override `stream`)
- Test: `lumina-core/src/test/java/io/lumina/ai/TokenStreamTest.java`, `EchoChatClientTest.java` (extend)

**Interfaces:**
- Consumes: `ChatClient` (Phase 1)
- Produces:
  - `TokenStream extends Iterable<String>`; statics `TokenStream of(String)`, `TokenStream fromIterable(Iterable<String>)`
  - `ChatClient.stream(String) : TokenStream` default = `TokenStream.of(prompt(input))`
  - Echo `stream()` splits the echoed text into word chunks (each chunk includes its trailing space so concatenation reproduces the whole string)

- [ ] **Step 1: Write failing tests**

```java
package io.lumina.ai;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TokenStreamTest {
    @Test
    void ofYieldsSingleChunk() {
        List<String> chunks = new ArrayList<>();
        TokenStream.of("hello world").forEach(chunks::add);
        assertThat(chunks).containsExactly("hello world");
    }

    @Test
    void fromIterablePreservesChunks() {
        List<String> chunks = new ArrayList<>();
        TokenStream.fromIterable(List.of("a", "b", "c")).forEach(chunks::add);
        assertThat(chunks).containsExactly("a", "b", "c");
    }

    @Test
    void chunksConcatenateToWhole() {
        StringBuilder sb = new StringBuilder();
        TokenStream.fromIterable(List.of("Hel", "lo")).forEach(sb::append);
        assertThat(sb.toString()).isEqualTo("Hello");
    }
}
```

Extend `EchoChatClientTest`:

```java
@Test
void streamChunksConcatenateToPromptResult() {
    ChatClient client = ChatClients.echo();
    StringBuilder sb = new StringBuilder();
    client.stream("hello world").forEach(sb::append);
    assertThat(sb.toString()).isEqualTo(client.prompt("hello world"));
}

@Test
void streamEmitsMultipleChunksForMultiWord() {
    java.util.List<String> chunks = new java.util.ArrayList<>();
    ChatClients.echo().stream("one two three").forEach(chunks::add);
    assertThat(chunks.size()).isGreaterThan(1);
}
```

- [ ] **Step 2: Run to verify fail**

Run: `mvn -q -pl lumina-core test -Dtest=TokenStreamTest,EchoChatClientTest`
Expected: FAIL (TokenStream missing; `stream` default returns single chunk so multi-chunk test fails).

- [ ] **Step 3: Implement**

`TokenStream.java`:

```java
package io.lumina.ai;

import java.util.List;
import java.util.Objects;

/**
 * A blocking, forward-only source of text chunks produced by a {@link ChatClient}.
 * Iteration blocks until the next chunk is available or the stream ends.
 */
public interface TokenStream extends Iterable<String> {

    /**
     * Wraps a complete string as a single-chunk stream, for non-streaming clients.
     *
     * @param whole full text; must not be null
     * @return a stream yielding {@code whole} as one chunk
     */
    static TokenStream of(String whole) {
        Objects.requireNonNull(whole, "whole");
        return fromIterable(List.of(whole));
    }

    /**
     * Wraps an existing iterable of chunks as a token stream.
     *
     * @param chunks chunk source; must not be null
     * @return a stream yielding {@code chunks} in order
     */
    static TokenStream fromIterable(Iterable<String> chunks) {
        Objects.requireNonNull(chunks, "chunks");
        return chunks::iterator;
    }
}
```

`ChatClient.stream` default:

```java
    /**
     * Streams the completion as chunks. The default yields the whole {@link #prompt(String)}
     * result as a single chunk, preserving source and binary compatibility for existing clients.
     *
     * @param input user prompt; never null
     * @return token stream of the reply; never null
     */
    default TokenStream stream(String input) {
        return TokenStream.of(prompt(input));
    }
```

`EchoChatClient.stream` override — split on spaces, keep trailing space on each chunk except the last so concatenation is lossless:

```java
    @Override
    public TokenStream stream(String input) {
        String reply = prompt(input);
        java.util.List<String> chunks = new java.util.ArrayList<>();
        String[] words = reply.split(" ", -1);
        for (int i = 0; i < words.length; i++) {
            chunks.add(i < words.length - 1 ? words[i] + " " : words[i]);
        }
        return TokenStream.fromIterable(chunks);
    }
```

- [ ] **Step 4: Run to verify pass**

Run: `mvn -q -pl lumina-core test -Dtest=TokenStreamTest,EchoChatClientTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add lumina-core
git commit -m "$(cat <<'EOF'
feat(core): add TokenStream and streaming ChatClient.stream

Introduce blocking token source with a streaming echo for offline demos.
EOF
)"
```

---

### Task 3: Ui.ai(TokenStream) contract (core)

**Files:**
- Modify: `lumina-core/src/main/java/io/lumina/ui/Ui.java` (add `String ai(TokenStream)`)
- Modify: `lumina-core/src/test/java/io/lumina/ui/UiSignatureTest.java` (FakeUi implements new method)

**Interfaces:**
- Consumes: `TokenStream`
- Produces: `Ui.ai(TokenStream tokens) : String` returning the fully accumulated text

- [ ] **Step 1: Add the method to the FakeUi test first (RED)**

In `UiSignatureTest.FakeUi` add:

```java
        @Override public String ai(io.lumina.ai.TokenStream tokens) {
            StringBuilder sb = new StringBuilder();
            tokens.forEach(sb::append);
            return sb.toString();
        }
```

Add an assertion:

```java
    @Test
    void aiTokenStreamReturnsAccumulatedText() {
        Ui ui = new FakeUi();
        assertThat(ui.ai(io.lumina.ai.TokenStream.fromIterable(java.util.List.of("a", "b"))))
                .isEqualTo("ab");
    }
```

- [ ] **Step 2: Run to verify fail**

Run: `mvn -q -pl lumina-core test -Dtest=UiSignatureTest`
Expected: FAIL (compile error: `Ui` has no `ai(TokenStream)`).

- [ ] **Step 3: Implement**

Add to `Ui`:

```java
    /**
     * Renders an assistant chat message whose text streams in chunk-by-chunk. Blocks until the
     * stream completes and returns the fully accumulated text (for history persistence). The
     * accumulated text also becomes the message node's content.
     *
     * @param tokens streamed reply chunks; must not be null
     * @return the fully accumulated reply text
     */
    String ai(io.lumina.ai.TokenStream tokens);
```

(Use a normal import at the top of `Ui.java` rather than the fully qualified name.)

- [ ] **Step 4: Run to verify pass**

Run: `mvn -q -pl lumina-core test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add lumina-core
git commit -m "$(cat <<'EOF'
feat(core): add streaming Ui.ai(TokenStream) returning accumulated text
EOF
)"
```

---

### Task 4: StreamBridge + streaming in UiBinder (runtime)

**Files:**
- Create: `lumina-runtime/src/main/java/io/lumina/runtime/StreamBridge.java`
- Modify: `lumina-runtime/src/main/java/io/lumina/runtime/UiBinder.java`
- Test: `lumina-runtime/src/test/java/io/lumina/runtime/UiBinderStreamingTest.java`

**Interfaces:**
- Consumes: `TokenStream`, `ComponentNode`, `ComponentTypes`
- Produces:
  - `StreamBridge` interface (runtime-internal) with:
    - `void flushBefore(List<ComponentNode> childrenSoFar)` — flush interim structural patch
    - `void streamStart(String nodeId)` / `void streamAppend(String nodeId, String text)` / `void streamEnd(String nodeId)`
    - marks a node id as streamed (for suppression)
  - `UiBinder.ai(TokenStream)` implementation + `UiBinder(SessionState, StreamBridge)` constructor (keep the single-arg constructor delegating to `StreamBridge.NOOP`)
  - `UiBinder.streamedNodeIds() : Set<String>`

Note: this task introduces no public `FrameSink`; the transport-facing sink is `RunSink`, introduced in Task 5. The `StreamBridge` here is the binder-facing, package-private hook only.

- [ ] **Step 1: Write failing test (bridge captures events)**

```java
package io.lumina.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumina.ai.TokenStream;
import io.lumina.model.ComponentNode;
import io.lumina.model.ComponentTypes;
import io.lumina.session.internal.SessionState;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class UiBinderStreamingTest {

    private static final class RecordingBridge implements StreamBridge {
        final List<String> events = new ArrayList<>();
        int flushes = 0;

        @Override public void flushBefore(List<ComponentNode> childrenSoFar) { flushes++; }
        @Override public void streamStart(String nodeId) { events.add("start:" + nodeId); }
        @Override public void streamAppend(String nodeId, String text) { events.add("append:" + text); }
        @Override public void streamEnd(String nodeId) { events.add("end:" + nodeId); }
    }

    @Test
    void aiStreamEmitsFlushStartAppendsEndAndReturnsText() {
        SessionState session = new SessionState();
        RecordingBridge bridge = new RecordingBridge();
        UiBinder ui = new UiBinder(session, bridge);

        String result = ui.ai(TokenStream.fromIterable(List.of("Hel", "lo")));

        assertThat(result).isEqualTo("Hello");
        assertThat(bridge.flushes).isEqualTo(1);
        assertThat(bridge.events).containsSubsequence("append:Hel", "append:lo");
        assertThat(bridge.events.get(0)).startsWith("start:");
        assertThat(bridge.events.get(bridge.events.size() - 1)).startsWith("end:");
    }

    @Test
    void streamedAiNodeCarriesFullContentAndIsMarked() {
        SessionState session = new SessionState();
        RecordingBridge bridge = new RecordingBridge();
        UiBinder ui = new UiBinder(session, bridge);

        ui.ai(TokenStream.fromIterable(List.of("Hel", "lo")));
        ComponentNode root = ui.buildRoot();

        ComponentNode ai = root.children().stream()
                .filter(n -> n.type().equals(ComponentTypes.AI_MESSAGE)).findFirst().orElseThrow();
        assertThat(ai.props().get("content")).isEqualTo("Hello");
        assertThat(ui.streamedNodeIds()).contains(ai.id());
    }

    @Test
    void nonStreamingBinderStillWorks() {
        UiBinder ui = new UiBinder(new SessionState());
        ui.ai("done");
        assertThat(ui.buildRoot().children()).hasSize(1);
    }
}
```

- [ ] **Step 2: Run to verify fail**

Run: `mvn -q -pl lumina-runtime -am test -Dtest=UiBinderStreamingTest`
Expected: FAIL (types/methods missing).

- [ ] **Step 3: Implement**

`StreamBridge.java` (runtime-internal, package-private):

```java
package io.lumina.runtime;

import io.lumina.model.ComponentNode;
import java.util.List;

/**
 * Runtime hook the {@link UiBinder} calls while streaming an {@code ai_message}: it flushes an
 * interim structural patch (so the client has the target element) and emits text-only stream
 * frames (ADR-006). The default no-op bridge makes streaming behave like a normal append.
 */
interface StreamBridge {
    StreamBridge NOOP = new StreamBridge() {
        @Override public void flushBefore(List<ComponentNode> childrenSoFar) { }
        @Override public void streamStart(String nodeId) { }
        @Override public void streamAppend(String nodeId, String text) { }
        @Override public void streamEnd(String nodeId) { }
    };

    void flushBefore(List<ComponentNode> childrenSoFar);
    void streamStart(String nodeId);
    void streamAppend(String nodeId, String text);
    void streamEnd(String nodeId);
}
```

Modify `UiBinder`:
- Add field `private final StreamBridge stream;` and `private final Set<String> streamedIds = new LinkedHashSet<>();`
- New constructor `public UiBinder(SessionState session, StreamBridge stream)`; existing `UiBinder(SessionState)` delegates with `StreamBridge.NOOP`.
- Implement `ai(TokenStream)`:

```java
    @Override
    public String ai(TokenStream tokens) {
        Objects.requireNonNull(tokens, "tokens");
        String key = nextKey(ComponentTypes.AI_MESSAGE);
        // add placeholder (empty content) so the interim flush creates the element
        children.add(new ComponentNode(key, ComponentTypes.AI_MESSAGE, Map.of(CONTENT, ""), List.of()));
        stream.flushBefore(List.copyOf(children));
        stream.streamStart(key);
        StringBuilder acc = new StringBuilder();
        for (String chunk : tokens) {
            acc.append(chunk);
            stream.streamAppend(key, chunk);
        }
        stream.streamEnd(key);
        streamedIds.add(key);
        // replace placeholder with full-content node
        int last = children.size() - 1;
        children.set(last, new ComponentNode(key, ComponentTypes.AI_MESSAGE, Map.of(CONTENT, acc.toString()), List.of()));
        return acc.toString();
    }

    Set<String> streamedNodeIds() {
        return Set.copyOf(streamedIds);
    }
```

(Import `java.util.Set`, `java.util.LinkedHashSet`.)

Note: the placeholder is the last child only if `ui.ai` appends nothing else between add and replace — which holds because it is synchronous within the method.

- [ ] **Step 4: Run to verify pass**

Run: `mvn -q -pl lumina-runtime -am test -Dtest=UiBinderStreamingTest,UiBinderTest`
Expected: PASS (existing `UiBinderTest` unaffected).

- [ ] **Step 5: Commit**

```bash
git add lumina-runtime
git commit -m "$(cat <<'EOF'
feat(runtime): stream ai_message via StreamBridge in UiBinder

Emit interim flush + start/append/end while accumulating full content.
EOF
)"
```

---

### Task 5: AppRunner streaming orchestration + suppression (runtime)

**Files:**
- Modify: `lumina-runtime/src/main/java/io/lumina/runtime/AppRunner.java`
- Modify: `lumina-runtime/src/main/java/io/lumina/runtime/SessionHandle.java` (add `submit(Intent, RunSink)`)
- Test: `lumina-runtime/src/test/java/io/lumina/runtime/AppRunnerStreamingTest.java`

**Interfaces:**
- Consumes: `RunSink`, `StreamBridge`, `UiBinder`, `TreeDiffer`, `RunResult`; `ProtocolCodec` is NOT visible here (runtime has no web dep) — so frames are encoded by a runtime-level encoder passed in, OR AppRunner emits structured events. **Decision:** the interim flush produces a `RunResult` (snapshot/patched) and stream frames are plain JSON strings built by a small runtime `StreamFrames` helper (no Jackson; hand-built minimal JSON with escaping). The web layer maps the interim `RunResult` to its own `ProtocolCodec` output — see below.
- Produces:
  - `AppRunner.run(LuminaApp, SessionState, Intent, RunSink)` where `RunSink` bundles interim delivery + frame sending (see Step 3).
  - `SessionHandle.submit(Intent, RunSink) : CompletableFuture<RunResult>`; existing `submit(Intent)` delegates with `RunSink.NOOP`.

**Design note (encoding boundary):** `lumina-runtime` must not depend on `lumina-web`/Jackson. Interim structural patches are therefore delivered as `RunResult` objects through a callback the web layer encodes, while text `stream` frames are simple, well-defined JSON built by a tiny runtime helper `StreamFrames` (escaping `"` and `\\` and control chars). This keeps Jackson in `lumina-web` while letting the runtime push frames.

- [ ] **Step 1: Write failing test**

```java
package io.lumina.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumina.LuminaApp;
import io.lumina.ai.ChatClients;
import io.lumina.model.ComponentTypes;
import io.lumina.session.internal.SessionState;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AppRunnerStreamingTest {

    @Test
    void streamingRunFlushesInterimThenEmitsFramesThenSuppressesFinalUpdate() {
        LuminaApp app = ui -> {
            String p = ui.chatInput();
            if (p != null) {
                ui.user(p);
                ui.ai(ChatClients.echo().stream(p));
            }
        };
        SessionState session = new SessionState();
        AppRunner runner = new AppRunner();

        // connect
        List<String> frames = new ArrayList<>();
        List<RunResult> interims = new ArrayList<>();
        RunResult connect = runner.run(app, session, Intent.connect(),
                new CapturingSink(interims, frames));
        assertThat(connect.fullSnapshot()).isTrue();

        // find chat input id
        String chatId = connect.root().children().stream()
                .filter(n -> n.type().equals(ComponentTypes.CHAT_INPUT)).findFirst().orElseThrow().id();

        frames.clear();
        interims.clear();
        RunResult result = runner.run(app, session, Intent.chatSubmit(chatId, "hi there"),
                new CapturingSink(interims, frames));

        // interim flush created user + empty ai; stream frames carried the text
        assertThat(interims).isNotEmpty();
        assertThat(frames).anyMatch(f -> f.contains("\"op\":\"start\""));
        assertThat(frames).anyMatch(f -> f.contains("\"op\":\"append\""));
        assertThat(frames).anyMatch(f -> f.contains("\"op\":\"end\""));
        // final result must NOT re-send the streamed ai_message content as a patch op
        assertThat(result.patches()).noneMatch(op ->
                "UPDATE_PROPS".equals(op.op()) && op.path() != null && op.path().contains("ai"));
    }

    private record CapturingSink(List<RunResult> interims, List<String> frames) implements RunSink {
        @Override public void deliverInterim(RunResult interim) { interims.add(interim); }
        @Override public void sendFrame(String json) { frames.add(json); }
    }
}
```

(If asserting on `op.path().contains("ai")` proves brittle, assert instead that no patch op targets an id in the run's streamed set — expose the streamed ids on `RunResult` as `Set<String> streamedIds()` and assert patches don't touch them. Prefer this if the path scheme differs.)

- [ ] **Step 2: Run to verify fail**

Run: `mvn -q -pl lumina-runtime -am test -Dtest=AppRunnerStreamingTest`
Expected: FAIL.

- [ ] **Step 3: Implement**

Add `RunSink` (runtime, public):

```java
package io.lumina.runtime;

/**
 * Sink for a streaming-capable run: receives interim structural results (encoded by the
 * transport) and raw text stream frames (ADR-006).
 */
public interface RunSink {
    /** Delivers an interim structural result (e.g. the flush that creates a streaming node). */
    void deliverInterim(RunResult interim);

    /** Sends a raw {@code stream} frame JSON string. */
    void sendFrame(String json);

    /** A sink that discards everything (headless runs). */
    RunSink NOOP = new RunSink() {
        @Override public void deliverInterim(RunResult interim) { }
        @Override public void sendFrame(String json) { }
    };
}
```

Add `StreamFrames` helper (runtime, package-private) producing minimal JSON:

```java
package io.lumina.runtime;

final class StreamFrames {
    private StreamFrames() {}

    static String start(String id) {
        return "{\"type\":\"stream\",\"id\":" + quote(id) + ",\"op\":\"start\"}";
    }

    static String append(String id, String text) {
        return "{\"type\":\"stream\",\"id\":" + quote(id) + ",\"op\":\"append\",\"text\":" + quote(text) + "}";
    }

    static String end(String id) {
        return "{\"type\":\"stream\",\"id\":" + quote(id) + ",\"op\":\"end\"}";
    }

    private static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }
}
```

Modify `AppRunner`:
- Add `RunResult run(LuminaApp app, SessionState session, Intent intent, RunSink sink)`; keep the existing 3-arg method delegating with `RunSink.NOOP`.
- Implement a `StreamBridge` inside `run(...)` that:
  - `flushBefore(children)`: builds an interim root `new ComponentNode("root", ROOT, Map.of(), children)`, diffs `previousRoot` vs interim, delivers `RunResult.snapshot(interim)` if `previousRoot == null` else `RunResult.patched(interim, ops)` via `sink.deliverInterim(...)`, and advances `previousRoot = interim`.
  - `streamStart/append/end`: `sink.sendFrame(StreamFrames.start/append/end(...))`.
- Construct `UiBinder ui = new UiBinder(session, bridge)`.
- After `app.build(ui)`, `newRoot = ui.buildRoot()`; compute `patches = differ.diff(previousRoot, newRoot)` then **filter out** ops whose target node id ∈ `ui.streamedNodeIds()`. Set `previousRoot = newRoot`. Return `patched`/`snapshot` accordingly.
- To make suppression precise, add `Set<String> streamedIds` to `RunResult` (new component, default empty) OR filter by matching op node ids. **Decision:** add `streamedIds` to `RunResult` via a new canonical field with a compatible factory:
  - Extend record: `RunResult(ComponentNode root, List<PatchOp> patches, boolean fullSnapshot, String error, Set<String> streamedIds)`; keep all existing factories delegating with `Set.of()`; add `patched(root, patches, streamedIds)`.
  - `PatchOp` must expose the node id it targets. If `PatchOp` lacks a direct id, match on `node().id()` for ADD/REPLACE and on the child id encoded in `path`/`node` for others. **If PatchOp does not carry enough identity, add an `id` accessor** (inspect `PatchOp` first; extend minimally and compatibly).

Filtering:

```java
Set<String> streamed = ui.streamedNodeIds();
List<PatchOp> filtered = raw.stream()
        .filter(op -> !streamed.contains(targetId(op)))
        .toList();
```

where `targetId(op)` returns the affected node id (implement per `PatchOp` shape).

Modify `SessionHandle` — add a `RunSink` overload (the runtime's public streaming API); the web layer supplies a `RunSink` that encodes interim `RunResult`s with `ProtocolCodec` and forwards text frames. `SessionHandle` itself never encodes (no Jackson in runtime):

```java
    public CompletableFuture<RunResult> submit(Intent intent, RunSink sink) {
        Objects.requireNonNull(intent, "intent");
        Objects.requireNonNull(sink, "sink");
        return executor.submit(() -> runner.run(app, session, intent, sink));
    }
```

Keep the existing `submit(Intent)` delegating with `RunSink.NOOP`. Inside `AppRunner.run(...)`, the `StreamBridge` it builds calls `sink.deliverInterim(...)` and `sink.sendFrame(...)` directly.

- [ ] **Step 4: Run to verify pass**

Run: `mvn -q -pl lumina-runtime -am test -Dtest=AppRunnerStreamingTest,AppRunnerTest,UiBinderStreamingTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add lumina-runtime
git commit -m "$(cat <<'EOF'
feat(runtime): orchestrate streaming runs with interim flush and suppression

Add RunSink, deliver interim structural patches, emit stream frames, and
suppress redundant final ops for streamed nodes.
EOF
)"
```

---

### Task 6: Web wiring — encode stream frames + interim patches

**Files:**
- Modify: `lumina-web/src/main/java/io/lumina/web/LuminaWebSocketEndpoint.java`
- Test: `lumina-web/src/test/java/io/lumina/web/LuminaServerIT.java` (add streaming IT)

**Interfaces:**
- Consumes: `SessionHandle.submit(Intent, RunSink)`, `RunResult`, `ProtocolCodec`
- Produces: a `RunSink` implementation that encodes interim `RunResult`s with `ProtocolCodec` (snapshot vs patch) and forwards stream frames verbatim to the WS session; final `RunResult` replied as today

- [ ] **Step 1: Write failing IT**

Add to `LuminaServerIT` a test that starts a server whose app streams (`ui.ai(ChatClients.echo().stream(prompt))`), connects a WS client, submits a chat intent, and collects frames until an `end`:

```java
@Test
void websocketStreamsAiMessageInChunks() throws Exception {
    LuminaServer server = LuminaServer.start(ui -> {
        String p = ui.chatInput();
        if (p != null) {
            ui.user(p);
            ui.ai(io.lumina.ai.ChatClients.echo().stream(p));
        }
    }, LuminaServerConfig.builder().port(0).build());
    try {
        // connect, read snapshot, extract chat_input id
        // send {"type":"intent","name":"submit_chat","targetId":<id>,"payload":{"value":"hi there"}}
        // collect text frames until one contains "\"op\":\"end\""
        // assert: at least one "\"op\":\"start\"", multiple "\"op\":\"append\"", one "\"op\":\"end\""
        // assert: concatenation of append "text" fields equals "Echo: hi there"
        // assert: an interim patch/snapshot message arrived that ADDs an ai_message node
    } finally {
        server.stop();
    }
}
```

Implement with the same JDK `HttpClient` WebSocket helper the other ITs use; accumulate messages in a thread-safe list with a latch that counts down on the `end` frame.

- [ ] **Step 2: Run to verify fail**

Run: `mvn -q -pl lumina-web -am test -Dtest=LuminaServerIT#websocketStreamsAiMessageInChunks`
Expected: FAIL (streaming not wired; `submit(Intent, RunSink)` not used).

- [ ] **Step 3: Implement web RunSink**

In `LuminaWebSocketEndpoint`, replace the plain `submit(intent)` calls with `submit(intent, sink)` where `sink` is:

```java
    private RunSink sinkFor(Session session) {
        return new RunSink() {
            @Override public void deliverInterim(RunResult interim) {
                if (interim.hasError()) {
                    session.sendText(ProtocolCodec.toErrorJson(APPLICATION_ERROR), Callback.NOOP);
                    return;
                }
                String json = interim.fullSnapshot()
                        ? ProtocolCodec.toSnapshotJson(interim.root())
                        : ProtocolCodec.toPatchJson(interim.patches());
                session.sendText(json, Callback.NOOP);
            }
            @Override public void sendFrame(String json) {
                session.sendText(json, Callback.NOOP);
            }
        };
    }
```

Use it in both `onOpen` (connect) and `onMessage`. The final `whenComplete((result, error) -> reply(session, result, error))` stays; `reply` already suppresses-then-sends the (now filtered) patch/snapshot.

Guarantee frame ordering: since the session executor runs the whole run (including interim `deliverInterim` and `sendFrame`) on one thread before the future completes, and `whenComplete` fires afterward, interim + stream frames are sent before the final reply — matching ADR-006 order.

- [ ] **Step 4: Run to verify pass**

Run: `mvn -q -pl lumina-web -am test -Dtest=LuminaServerIT`
Expected: PASS (all ITs including the new one).

- [ ] **Step 5: Commit**

```bash
git add lumina-web
git commit -m "$(cat <<'EOF'
feat(web): stream ai_message frames over WebSocket

Encode interim structural patches and forward stream start/append/end frames.
EOF
)"
```

---

### Task 7: Browser client — handle stream frames

**Files:**
- Modify: `lumina-web/src/main/resources/static/lumina-web/lumina-client.js`
- Modify: `lumina-web/src/main/resources/static/lumina-web/lumina.css` (typing indicator)

**Interfaces:**
- Consumes: `stream` frames `{type:"stream", id, op:"start"|"append"|"end", text?}`
- Produces: incremental text append to the `ai_message` element identified by `id`; typing indicator on start, cleared on end

- [ ] **Step 1: Add an id→element index and stream handling**

In the message handler (where `message.type === "snapshot" | "patch" | "error"` are handled), add:

```js
} else if (message.type === "stream") {
    this.applyStream(message);
}
```

Implement `applyStream({ id, op, text })`:
- Maintain `this.nodeElements` (Map from node id → rendered element) populated during `renderNode`/`applyPatch` (store `element.dataset.luminaId = node.id` and look up via a map or `querySelector('[data-lumina-id="..."]')`).
- `start`: find the target `lumina-ai-message` element by id; add a CSS class `streaming` (typing indicator).
- `append`: append `text` to the element's text content using `textContent +=` (XSS-safe); update the backing node's `props.content` so later diffs are consistent.
- `end`: remove the `streaming` class.

Because the target element is created by the preceding interim patch, it exists before `start`. If not found (defensive), no-op.

- [ ] **Step 2: Typing indicator CSS**

Add a `.streaming::after { content: "▍"; animation: ... }` (blinking caret) style scoped to the ai message element. Keep it minimal.

- [ ] **Step 3: Verify served + syntax**

Run: `node --check lumina-web/src/main/resources/static/lumina-web/lumina-client.js`
Expected: no output (valid JS).

Run: `mvn -q -pl lumina-web -am test -Dtest=LuminaServerIT`
Expected: PASS (server still serves updated assets; existing asset-serving assertions hold).

- [ ] **Step 4: Commit**

```bash
git add lumina-web/src/main/resources/static/lumina-web
git commit -m "$(cat <<'EOF'
feat(web): render streaming ai_message chunks in the browser client

Append stream frames by node id with a typing indicator.
EOF
)"
```

---

### Task 8: lumina-spring-ai module (Spring AI adapter)

**Files:**
- Create: `lumina-spring-ai/pom.xml`
- Create: `lumina-spring-ai/src/main/java/io/lumina/springai/SpringAiChatClient.java`
- Create: `lumina-spring-ai/src/main/java/io/lumina/springai/LuminaSpringAiAutoConfiguration.java`
- Create: `lumina-spring-ai/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Modify: root `pom.xml` (`<module>`, Spring AI BOM in `dependencyManagement`)
- Test: `lumina-spring-ai/src/test/java/io/lumina/springai/SpringAiChatClientTest.java`

**Interfaces:**
- Consumes: `io.lumina.ai.ChatClient`, `io.lumina.ai.TokenStream`, Spring AI `ChatClient`/`ChatModel` (reactive `Flux<String>`)
- Produces:
  - `SpringAiChatClient implements io.lumina.ai.ChatClient` wrapping a Spring AI chat client; `prompt` blocking; `stream` bridges `Flux<String>` → `TokenStream` via a bounded `BlockingQueue<String>` drained by iteration, with an end sentinel and error propagation
  - Optional `@AutoConfiguration` registering a `ChatClient` bean when a Spring AI chat model is present

- [ ] **Step 1: POM + module registration**

Add `lumina-spring-ai` to root `<modules>`; add the Spring AI BOM to `dependencyManagement` (property `spring-ai.version`, e.g. `1.0.1`). Module depends on `lumina-core`, `spring-ai-client-chat` (or equivalent), and test deps (`junit`, `assertj`, `reactor-core` for `Flux` in tests).

- [ ] **Step 2: Write failing test (Flux → TokenStream bridge)**

```java
package io.lumina.springai;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumina.ai.TokenStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

class SpringAiChatClientTest {
    @Test
    void bridgesFluxChunksInOrder() {
        TokenStream stream = SpringAiChatClient.toTokenStream(Flux.just("Hel", "lo", "!"));
        List<String> chunks = new ArrayList<>();
        stream.forEach(chunks::add);
        assertThat(chunks).containsExactly("Hel", "lo", "!");
    }

    @Test
    void propagatesEmptyFluxAsNoChunks() {
        List<String> chunks = new ArrayList<>();
        SpringAiChatClient.toTokenStream(Flux.empty()).forEach(chunks::add);
        assertThat(chunks).isEmpty();
    }
}
```

- [ ] **Step 3: Run to verify fail**

Run: `mvn -q -pl lumina-spring-ai -am test -Dtest=SpringAiChatClientTest`
Expected: FAIL.

- [ ] **Step 4: Implement**

`SpringAiChatClient`:
- Constructor takes a Spring AI chat client abstraction.
- `prompt(input)`: call Spring AI blocking `.call().content()` (adapt to the Spring AI API in use).
- Static package-visible `toTokenStream(Flux<String> flux)`:

```java
static TokenStream toTokenStream(Flux<String> flux) {
    BlockingQueue<Object> queue = new LinkedBlockingQueue<>();
    Object end = new Object();
    flux.subscribe(
        item -> queue.add(item),
        error -> queue.add(new Err(error)),
        () -> queue.add(end));
    return () -> new Iterator<>() {
        String next;
        boolean done;
        // hasNext(): take() from queue; if end -> done=true,false; if Err -> throw LuminaException; else buffer
        // next(): return buffered
    };
}
```

Implement the iterator carefully (buffer one element in `hasNext`). Wrap errors in `io.lumina.LuminaException`. `stream(input)` calls the Spring AI streaming API to obtain `Flux<String>` and returns `toTokenStream(flux)`.

`LuminaSpringAiAutoConfiguration`: `@AutoConfiguration`, `@ConditionalOnClass` (Spring AI), `@ConditionalOnBean` of the Spring AI chat model, `@ConditionalOnMissingBean(io.lumina.ai.ChatClient.class)` → register `SpringAiChatClient`. Register it in the `AutoConfiguration.imports` file.

- [ ] **Step 5: Run to verify pass**

Run: `mvn -q -pl lumina-spring-ai -am test`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add lumina-spring-ai pom.xml
git commit -m "$(cat <<'EOF'
feat(spring-ai): add Spring AI ChatClient adapter with Flux bridge

Bridge Spring AI streaming Flux to Lumina TokenStream; optional auto-config.
EOF
)"
```

---

### Task 9: Streaming example + README + full verification

**Files:**
- Create: `lumina-examples/src/main/java/io/lumina/examples/streaming/StreamingChatApp.java`
- Create: `lumina-examples/src/main/java/io/lumina/examples/streaming/StreamingChatMain.java`
- Modify: `lumina-examples/README.md`, root `README.md`
- Test: `lumina-examples/src/test/java/io/lumina/examples/streaming/StreamingChatAppTest.java`

**Interfaces:**
- Consumes: `LuminaApp`, `Ui.ai(TokenStream)`, `ChatClients.echo`, `LuminaServer`
- Produces: runnable streaming example (<20 lines of app logic) + smoke test

- [ ] **Step 1: Implement the example (mirror spec §1)**

`StreamingChatApp` with history persistence via the returned accumulated text; `StreamingChatMain` starts `LuminaServer`.

- [ ] **Step 2: Smoke test**

Reuse the streaming IT approach (or a headless `AppRunner`/`SessionHandle` test) asserting the streamed reply reconstructs `Echo: <prompt>`.

- [ ] **Step 3: Docs**

Add a "Streaming chat" section to both READMEs with the run command:

```bash
mvn -q -pl lumina-examples -am install
mvn -q -pl lumina-examples exec:java -Dexec.mainClass=io.lumina.examples.streaming.StreamingChatMain
```

- [ ] **Step 4: Full verification**

Run: `mvn -q clean test`
Expected: BUILD SUCCESS.

Run: `mvn -q clean package`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add lumina-examples README.md
git commit -m "$(cat <<'EOF'
feat(examples): add streaming chat example and docs

Demonstrate ui.ai(TokenStream) with offline echo streaming.
EOF
)"
```

---

## Spec coverage checklist

| Spec item | Task |
|-----------|------|
| Version bump 0.2.0 | 1 |
| ADR-006 | 1 |
| TokenStream + streaming echo + ChatClient.stream | 2 |
| Ui.ai(TokenStream): String | 3 |
| Runtime RunSink + streaming binder | 4, 5 |
| Interim flush + suppression | 5 |
| stream wire frames | 5 (encode), 6 (transport) |
| Client stream handling | 7 |
| Spring AI adapter + Flux bridge + auto-config | 8 |
| Streaming example <20 lines | 9 |
| Tests from the start | 2–9 |
| Javadoc on new public APIs | 2–8 |
| Binary compatibility (defaults/overloads) | 2, 3, 5 |

## Self-review notes (author)

- **Sink layering:** the binder-facing hook is the package-private `StreamBridge` (Task 4); the transport-facing sink is the public `RunSink` (Task 5), which the web layer implements to encode interim structural patches with `ProtocolCodec` and forward text frames. `AppRunner` builds a `StreamBridge` that delegates to the `RunSink`. No `FrameSink` type ships.
- **PatchOp identity for suppression:** Task 5 requires mapping a `PatchOp` to the node id it affects. Implementer must inspect `PatchOp` first; if it lacks an id accessor, add one minimally and compatibly. Prefer suppressing by `RunResult.streamedIds()` membership over string-matching paths.
- **Ordering:** interim flush + stream frames are emitted on the session executor thread during the run, before the future completes, so they precede the final reply (ADR-006 order) without extra synchronization.
- **Reactive isolation:** `reactor-core`/Spring AI appear only in `lumina-spring-ai`; core/runtime stay clean (enforced by not adding those deps to their POMs).
