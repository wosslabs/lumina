# Lumina Nested Layout & Composition Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship nested `ComponentNode` trees and Streamlit-basic layout primitives (`container`, `columns`, `sidebar`, `expander`) with block-callback authoring, expander session widget state, and thin-client rendering.

**Architecture:** Replace `UiBinder`'s flat root list with a frame stack; layout methods push frames and emit layout nodes as parents. `columns(n, cols)` exposes `n` scoped `Ui` views (one per column) so `cols[i]` can be called in any order. Expander open/closed uses `WidgetState` + `expander_toggle` intent. Client registers layout custom elements; diff/wire unchanged except additive intent.

**Tech Stack:** Java 25, JUnit 5, AssertJ, Maven, vanilla Web Components client, existing Jetty/WebSocket stack.

## Global Constraints

- Spec: `docs/superpowers/specs/2026-07-20-lumina-nested-layout-design.md`.
- Version target: **`0.4.0-SNAPSHOT`** (Task 7 only).
- Additive API only: existing `Ui` methods unchanged; new layout methods + `expander_toggle` intent.
- Expander state in **`WidgetState`** keyed by expander node id — not `ui.state()` / `StateStore`.
- At most **one** `sidebar(...)` per `build()`; second call throws `LuminaException`.
- `columns(int n, …)` requires **`n >= 1`**; equal-width columns only.
- `lumina-core` / `lumina-runtime` stay free of Spring, Jetty, Servlet, and provider SDKs.
- Javadoc on all new public API members.
- Per-task commits authorized for this SDD run; do **not** push unless asked.
- Verification after logic changes: `mvn -q clean verify` (Task 7 gate).

---

### Task 1: Core layout API (`ComponentTypes`, `ComponentSpecs`, `Ui`)

**Files:**
- Modify: `lumina-components/src/main/java/io/lumina/components/ComponentSpecs.java`
- Modify: `lumina-core/src/main/java/io/lumina/model/ComponentTypes.java`
- Modify: `lumina-core/src/main/java/io/lumina/ui/Ui.java`
- Modify: `lumina-core/src/test/java/io/lumina/ui/UiSignatureTest.java`

**Interfaces:**
- Produces: wire type strings `container`, `columns`, `column`, `sidebar`, `expander`; prop keys `count`, `index`, `open` (via `ComponentSpecs`); `Ui` methods `container`, `columns`, `sidebar`, `expander`.

- [ ] **Step 1: Add layout prop constants to `ComponentSpecs`**

```java
/** Property containing the number of columns in a {@code columns} node. */
public static final String COUNT = "count";
/** Property containing a column's zero-based index. */
public static final String INDEX = "index";
/** Property containing whether an expander is open. */
public static final String OPEN = "open";
```

- [ ] **Step 2: Add layout type constants to `ComponentTypes`**

```java
/** Generic block container. */
public static final String CONTAINER = "container";
/** Row of equal-width columns. */
public static final String COLUMNS = "columns";
/** Single column slot inside a {@link #COLUMNS} row. */
public static final String COLUMN = "column";
/** Left navigation rail (at most one per build). */
public static final String SIDEBAR = "sidebar";
/** Collapsible section with persisted open state. */
public static final String EXPANDER = "expander";
```

- [ ] **Step 3: Add layout methods to `Ui.java`**

Add imports: `java.util.function.Consumer`.

Add methods with Javadoc (copy from spec §4):

```java
void container(Consumer<Ui> body);
void columns(int n, Consumer<Ui[]> cols);
void sidebar(Consumer<Ui> body);
boolean expander(String label, Consumer<Ui> body);
```

- [ ] **Step 4: Update `UiSignatureTest.FakeUi`**

Add stub implementations:

```java
@Override public void container(Consumer<Ui> body) { body.accept(this); }
@Override public void columns(int n, Consumer<Ui[]> cols) {
    Ui[] array = new Ui[n];
    java.util.Arrays.fill(array, this);
    cols.accept(array);
}
@Override public void sidebar(Consumer<Ui> body) { body.accept(this); }
@Override public boolean expander(String label, Consumer<Ui> body) {
    body.accept(this);
    return false;
}
```

Add import `java.util.function.Consumer`.

- [ ] **Step 5: Compile lumina-core**

Run: `mvn -q -pl lumina-core,lumina-components -am clean test-compile`
Expected: BUILD SUCCESS (runtime will not compile until Task 2 — that is expected; scope this task to core+components only).

- [ ] **Step 6: Run lumina-core tests**

Run: `mvn -q -pl lumina-core -am test -Dsurefire.failIfNoSpecifiedTests=false`
Expected: `UiSignatureTest` passes.

- [ ] **Step 7: Commit**

```bash
git add lumina-components/src/main/java/io/lumina/components/ComponentSpecs.java \
  lumina-core/src/main/java/io/lumina/model/ComponentTypes.java \
  lumina-core/src/main/java/io/lumina/ui/Ui.java \
  lumina-core/src/test/java/io/lumina/ui/UiSignatureTest.java
git commit -m "feat: add layout component types and Ui API (container, columns, sidebar, expander)"
```

---

### Task 2: UiBinder frame stack (refactor flat list → frames)

**Files:**
- Modify: `lumina-runtime/src/main/java/io/lumina/runtime/UiBinder.java`
- Test: `lumina-runtime/src/test/java/io/lumina/runtime/UiBinderTest.java` (all existing tests must stay green)

**Interfaces:**
- Consumes: Task 1 `Ui` signatures (implement new methods as stubs throwing `UnsupportedOperationException` until Task 3, OR implement frame stack only in this task without layout methods — prefer **frame stack only**, layout stubs delegate to `unsupported()`).
- Produces: package-private frame stack; `addNode` targets `frames.peek().children()`; `buildRoot()` reads root frame children; streaming `ai(TokenStream)` appends to current frame.

- [ ] **Step 1: Introduce frame record and stack**

Replace field `private final List<ComponentNode> children = new ArrayList<>();` with:

```java
private record Frame(List<ComponentNode> children) {
    Frame() { this(new ArrayList<>()); }
}

private final Deque<Frame> frames = new ArrayDeque<>();
```

In constructor after `counters.push(...)`, add `frames.push(new Frame());`.

- [ ] **Step 2: Route `addNode` through current frame**

Change `addNode(String key, String type, Map<String, Object> props)` to:

```java
frames.peek().children().add(new ComponentNode(key, type, props, List.of()));
```

Change `buildRoot()` to:

```java
return new ComponentNode("root", ComponentTypes.ROOT, Map.of(), List.copyOf(frames.peek().children()));
```

- [ ] **Step 3: Fix streaming `ai(TokenStream)` to use current frame**

Replace direct `children.add` / `children.set(last, ...)` with frame peek helpers, e.g.:

```java
List<ComponentNode> current = frames.peek().children();
current.add(new ComponentNode(key, ComponentTypes.AI_MESSAGE, Map.of(CONTENT, ""), List.of()));
stream.flushBefore(List.copyOf(current));
// ...
int last = current.size() - 1;
current.set(last, new ComponentNode(key, ComponentTypes.AI_MESSAGE, Map.of(CONTENT, acc.toString()), List.of()));
```

- [ ] **Step 4: Add stub layout methods (until Task 3)**

```java
@Override public void container(Consumer<Ui> body) { throw new UnsupportedOperationException("Task 3"); }
@Override public void columns(int n, Consumer<Ui[]> cols) { throw new UnsupportedOperationException("Task 3"); }
@Override public void sidebar(Consumer<Ui> body) { throw new UnsupportedOperationException("Task 3"); }
@Override public boolean expander(String label, Consumer<Ui> body) { throw new UnsupportedOperationException("Task 3"); }
```

Add `import java.util.function.Consumer;`

- [ ] **Step 5: Run existing UiBinder tests**

Run: `mvn -q -pl lumina-runtime -am test -Dtest=UiBinderTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: all existing `UiBinderTest` methods PASS (flat apps behave identically).

- [ ] **Step 6: Commit**

```bash
git add lumina-runtime/src/main/java/io/lumina/runtime/UiBinder.java
git commit -m "refactor: UiBinder child-list frame stack (flat apps unchanged)"
```

---

### Task 3: Layout methods + scoped column Ui + tests

**Files:**
- Modify: `lumina-runtime/src/main/java/io/lumina/runtime/UiBinder.java`
- Test: `lumina-runtime/src/test/java/io/lumina/runtime/UiBinderTest.java`

**Interfaces:**
- Consumes: Task 2 frame stack; Task 1 types/props (`ComponentTypes.*`, `ComponentSpecs.COUNT`, `INDEX`, `OPEN`, `LABEL`).
- Produces: working `container`, `columns`, `sidebar`, `expander`; package-private `withFrame(Runnable)` or `ScopedUi` for column bodies.

**Implementation notes (required):**

1. **`withFrame(Runnable block)`** — push new `Frame` onto `frames`, run block, pop, return popped frame's `children` list.
2. **`container(body)`** — `String id = nextKey(CONTAINER)`; children = run body inside withFrame; add `ComponentNode(id, CONTAINER, Map.of(), children)` to parent frame.
3. **`sidebar(body)`** — if `sidebarUsed` throw `LuminaException("Only one sidebar is allowed per build()")`; set flag; same as container but type `SIDEBAR`.
4. **`columns(n, cols)`** — if `n < 1` throw `IllegalArgumentException`; `String columnsId = nextKey(COLUMNS)`; build `List<ComponentNode> columnNodes`:
   - For each `i` in `0..n-1`: create `List<ComponentNode> colChildren`; build `ScopedUi` (private static final class implementing `Ui`, delegating each method to outer `UiBinder.withActiveFrame(colFrame, () -> outer.method(...))` OR simpler: hold `UiBinder parent` + push/pop around every delegated call using a dedicated column frame pushed once at creation).
   - **Recommended `ScopedUi`:** constructed with parent `UiBinder`; on each `Ui` method call, parent executes `withinFrame(columnFrame, () -> parent.<method>(...))` where `columnFrame` is a pre-pushed frame object stored in the scope.
   - After `cols.accept(scopes)`: for each `i`, `columnNodes.add(new ComponentNode(colKey_i, COLUMN, Map.of(INDEX, i), colChildren_i))`.
   - Parent adds `new ComponentNode(columnsId, COLUMNS, Map.of(COUNT, n), columnNodes)`.
5. **`expander(label, body)`** — `String key = nextKey(EXPANDER)`; `boolean open = Boolean.TRUE.equals(session.widgets().value(key))`; body children via withFrame; add node with `Map.of(LABEL, label, OPEN, open)`; return `open`.

- [ ] **Step 1: Write failing layout tests**

Add to `UiBinderTest.java`:

```java
@Test
void containerNestsChildrenUnderContainerNode() {
    UiBinder ui = new UiBinder(new SessionState());
    ui.container(box -> box.text("inside"));
    ComponentNode root = ui.buildRoot();
    assertThat(root.children()).hasSize(1);
    ComponentNode container = root.children().getFirst();
    assertThat(container.type()).isEqualTo(ComponentTypes.CONTAINER);
    assertThat(container.children()).extracting(ComponentNode::type)
            .containsExactly(ComponentTypes.TEXT);
    assertThat(container.children().getFirst().props()).containsEntry("content", "inside");
}

@Test
void columnsCreatesEqualColumnSlots() {
    UiBinder ui = new UiBinder(new SessionState());
    ui.columns(2, cols -> {
        cols[0].markdown("L");
        cols[1].button("R");
    });
    ComponentNode columns = ui.buildRoot().children().getFirst();
    assertThat(columns.type()).isEqualTo(ComponentTypes.COLUMNS);
    assertThat(columns.props()).containsEntry("count", 2);
    assertThat(columns.children()).hasSize(2);
    assertThat(columns.children()).extracting(ComponentNode::type)
            .containsExactly(ComponentTypes.COLUMN, ComponentTypes.COLUMN);
    assertThat(columns.children().get(0).props()).containsEntry("index", 0);
    assertThat(columns.children().get(1).children()).extracting(ComponentNode::type)
            .containsExactly(ComponentTypes.BUTTON);
}

@Test
void secondSidebarThrows() {
    UiBinder ui = new UiBinder(new SessionState());
    ui.sidebar(s -> s.text("ok"));
    assertThatThrownBy(() -> ui.sidebar(s -> s.text("no")))
            .isInstanceOf(LuminaException.class)
            .hasMessageContaining("Only one sidebar");
}

@Test
void expanderReflectsWidgetStateOpenFlag() {
    SessionState session = new SessionState();
    UiBinder ui = new UiBinder(session);
    assertThat(ui.expander("Details", b -> b.text("hidden"))).isFalse();
    String expanderId = ui.buildRoot().children().getFirst().id();
    session.widgets().set(expanderId, true);
    ui = new UiBinder(session);
    assertThat(ui.expander("Details", b -> b.text("shown"))).isTrue();
    assertThat(ui.buildRoot().children().getFirst().props()).containsEntry("open", true);
}
```

Add imports: `io.lumina.runtime.LuminaException` if needed (check exception class name in codebase — use `io.lumina.runtime.LuminaException` or whatever exists).

- [ ] **Step 2: Run tests to verify failure**

Run: `mvn -q -pl lumina-runtime -am test -Dtest=UiBinderTest#containerNestsChildrenUnderContainerNode -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL (UnsupportedOperationException or assertion failure).

- [ ] **Step 3: Implement layout methods + ScopedUi per notes above**

Remove UnsupportedOperationException stubs. Import static `ComponentSpecs.COUNT`, `INDEX`, `OPEN`, `LABEL`.

- [ ] **Step 4: Run full UiBinderTest suite**

Run: `mvn -q -pl lumina-runtime -am test -Dtest=UiBinderTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: all PASS.

- [ ] **Step 5: Commit**

```bash
git add lumina-runtime/src/main/java/io/lumina/runtime/UiBinder.java \
  lumina-runtime/src/test/java/io/lumina/runtime/UiBinderTest.java
git commit -m "feat: nested layout binding (container, columns, sidebar, expander)"
```

---

### Task 4: Expander intent in runtime

**Files:**
- Modify: `lumina-runtime/src/main/java/io/lumina/runtime/AppRunner.java`
- Modify: `lumina-runtime/src/main/java/io/lumina/runtime/Intent.java`
- Test: `lumina-runtime/src/test/java/io/lumina/runtime/AppRunnerExpanderTest.java` (create)

**Interfaces:**
- Consumes: Task 3 expander node ids as `WidgetState` keys.
- Produces: `Intent.expanderToggle(String targetId)`; `AppRunner` handles `"expander_toggle"`.

- [ ] **Step 1: Write failing test**

Create `AppRunnerExpanderTest.java`:

```java
package io.lumina.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumina.LuminaApp;
import io.lumina.model.ComponentTypes;
import io.lumina.session.internal.SessionState;
import io.lumina.ui.Ui;
import org.junit.jupiter.api.Test;

class AppRunnerExpanderTest {
    @Test
    void expanderToggleFlipsOpenOnNextRun() {
        SessionState session = new SessionState();
        AppRunner runner = new AppRunner();
        LuminaApp app = ui -> ui.expander("More", body -> body.text("x"));

        runner.run(app, session, Intent.connect());
        String expanderId = runner.run(app, session, Intent.connect()).root().children().getFirst().id();
        assertThat(runner.run(app, session, Intent.connect()).root().children().getFirst().props())
                .containsEntry("open", false);

        runner.run(app, session, Intent.expanderToggle(expanderId));
        assertThat(runner.run(app, session, Intent.connect()).root().children().getFirst().props())
                .containsEntry("open", true);
    }
}
```

- [ ] **Step 2: Run test to verify failure**

Run: `mvn -q -pl lumina-runtime -am test -Dtest=AppRunnerExpanderTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL (unknown intent or missing factory).

- [ ] **Step 3: Add `Intent.expanderToggle` and `AppRunner` case**

In `Intent.java`:

```java
/**
 * Creates an expander open/closed toggle intent.
 *
 * @param targetId expander widget key
 * @return expander_toggle intent
 */
public static Intent expanderToggle(String targetId) {
    return new Intent("expander_toggle", targetId, Map.of());
}
```

Update Javadoc on record to mention `expander_toggle`.

In `AppRunner.applyIntent`:

```java
case "expander_toggle" -> {
    String key = requireTarget(intent);
    boolean open = Boolean.TRUE.equals(widgets.value(key));
    widgets.set(key, !open);
}
```

- [ ] **Step 4: Run test**

Run: `mvn -q -pl lumina-runtime -am test -Dtest=AppRunnerExpanderTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add lumina-runtime/src/main/java/io/lumina/runtime/Intent.java \
  lumina-runtime/src/main/java/io/lumina/runtime/AppRunner.java \
  lumina-runtime/src/test/java/io/lumina/runtime/AppRunnerExpanderTest.java
git commit -m "feat: handle expander_toggle intent in AppRunner"
```

---

### Task 5: Client layout rendering + expander toggle

**Files:**
- Modify: `lumina-web/src/main/resources/static/lumina-web/lumina-client.js`
- Modify: `lumina-web/src/main/resources/static/lumina-web/lumina.css`

**Interfaces:**
- Consumes: wire types from Task 1; `expander_toggle` intent shape from Task 4.
- Produces: registered custom elements for layout types; CSS for columns/sidebar/shell; expander sends WebSocket intent on toggle.

- [ ] **Step 1: Register layout elements in `ELEMENTS` map**

```javascript
container: "lumina-container",
columns: "lumina-columns",
column: "lumina-column",
sidebar: "lumina-sidebar",
expander: "lumina-expander",
```

- [ ] **Step 2: Add custom element classes**

Follow existing pattern (`LuminaText`, etc.). Minimal implementations:

- `LuminaContainer`, `LuminaColumns`, `LuminaColumn` — extend `LuminaNodeElement` or a new `LuminaLayoutElement` that renders children via existing child recursion in `renderNode` (layout elements use default child append in `renderNode` — ensure `render()` on layout elements does not wipe child DOM; may use empty `render()` that only sets class, letting `renderNode` append children).

**Important:** Read how `renderNode` works — it creates element, sets `node` property, appends children. Layout classes should implement `render()` that applies classes only; children appended by parent `renderNode` loop.

- [ ] **Step 3: Implement `LuminaExpander`**

Use `<details>` + `<summary>`:
- `render()` sets `open` from `this._node.props.open === true`
- On `toggle` event, send intent:

```javascript
sendIntent({ type: "intent", name: "expander_toggle", targetId: this._node.id });
```

Use existing `sendIntent` helper in the file (find the function used for button clicks).

- [ ] **Step 4: Register custom elements at bottom of file** (match existing `customElements.define` block).

- [ ] **Step 5: Add CSS to `lumina.css`**

```css
lumina-columns, .lumina-columns { display: flex; flex-direction: row; gap: 1rem; }
lumina-column, .lumina-column { flex: 1; min-width: 0; }
lumina-sidebar, .lumina-sidebar { /* left rail */ min-width: 12rem; }
/* App shell: lumina-app or #app flex when sidebar present — inspect existing root structure */
lumina-expander, .lumina-expander { display: block; margin: 0.5rem 0; }
```

Inspect `index.html` / client root id and apply shell layout accordingly.

- [ ] **Step 6: Manual smoke** — defer to Task 6 IT; compile not applicable for JS.

- [ ] **Step 7: Commit**

```bash
git add lumina-web/src/main/resources/static/lumina-web/lumina-client.js \
  lumina-web/src/main/resources/static/lumina-web/lumina.css
git commit -m "feat: client layout elements, shell CSS, and expander toggle intent"
```

---

### Task 6: Web integration tests

**Files:**
- Modify: `lumina-web/src/test/java/io/lumina/web/LuminaServerIT.java`
- Create: `lumina-web/src/test/java/io/lumina/web/ProtocolCodecExpanderTest.java`

**Interfaces:**
- Consumes: Tasks 3–5.

- [ ] **Step 1: Write ProtocolCodec test**

```java
package io.lumina.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumina.runtime.Intent;
import org.junit.jupiter.api.Test;

class ProtocolCodecExpanderTest {
    @Test
    void parsesExpanderToggleIntent() {
        Intent intent = ProtocolCodec.parseIntent(
                "{\"type\":\"intent\",\"name\":\"expander_toggle\",\"targetId\":\"auto:/expander#0\"}");
        assertThat(intent.name()).isEqualTo("expander_toggle");
        assertThat(intent.targetId()).isEqualTo("auto:/expander#0");
        assertThat(intent.payload()).isEmpty();
    }
}
```

- [ ] **Step 2: Add layout IT to `LuminaServerIT`**

Create a small test app inner class or use `lumina-examples` LayoutDemoApp if Task 7 already exists — **prefer inline test app** in IT file:

```java
public static final class LayoutITApp implements LuminaApp {
    @Override public void build(Ui ui) {
        ui.columns(2, cols -> {
            cols[0].text("A");
            cols[1].text("B");
        });
    }
}
```

After connect snapshot, assert JSON contains nested `"type":"column"` and `"type":"columns"`.

Add expander toggle IT: build app with one expander; send `expander_toggle` intent; assert subsequent patch/snapshot has `"open":true`.

Follow existing IT WebSocket client patterns in `LuminaServerIT`.

- [ ] **Step 3: Run web tests**

Run: `mvn -q -pl lumina-web -am test -Dtest=LuminaServerIT,ProtocolCodecExpanderTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add lumina-web/src/test/java/io/lumina/web/LuminaServerIT.java \
  lumina-web/src/test/java/io/lumina/web/ProtocolCodecExpanderTest.java
git commit -m "test: layout snapshot and expander_toggle integration coverage"
```

---

### Task 7: Example, version bump, docs, full verify

**Files:**
- Create: `lumina-examples/src/main/java/io/lumina/examples/layout/LayoutDemoApp.java`
- Create: `lumina-examples/src/main/java/io/lumina/examples/layout/LayoutDemoMain.java`
- Modify: `lumina-examples/README.md`
- Modify: `docs/adr/ADR-007-nested-component-tree.md` (one-line implementation note)
- Modify: all module `pom.xml` versions via `versions:set`

- [ ] **Step 1: Create `LayoutDemoApp`** (canonical example from spec §1)

- [ ] **Step 2: Create `LayoutDemoMain`** mirroring `HelloAiMain` / `StreamingChatMain` pattern.

- [ ] **Step 3: Update `lumina-examples/README.md`** with run instructions.

- [ ] **Step 4: Bump version to 0.4.0-SNAPSHOT**

Run: `mvn -q versions:set -DnewVersion=0.4.0-SNAPSHOT -DgenerateBackupPoms=false`

- [ ] **Step 5: Optional ADR-007 note** — append under Status: `Implemented in 0.4.0-SNAPSHOT (container, columns, sidebar, expander).`

- [ ] **Step 6: Full reactor verify**

Run: `mvn -q clean verify`
Expected: BUILD SUCCESS on JDK 25.

- [ ] **Step 7: Commit**

```bash
git add lumina-examples docs/adr/ADR-007-nested-component-tree.md pom.xml */pom.xml
git commit -m "feat: LayoutDemoApp example and bump to 0.4.0-SNAPSHOT"
```

---

## Self-Review

**Spec coverage:** Task 1 §4–5 types; Task 2–3 §6 binder; Task 4 §6.3+§7 intent; Task 5 §8 client; Task 6 §10 IT; Task 7 §14 example+version.

**Placeholder scan:** None.

**Type consistency:** `ComponentSpecs.OPEN/COUNT/INDEX`; `Intent.expanderToggle`; wire names match spec.

## Execution Handoff

Subagent-driven development: one implementer + reviewer per task; progress ledger at `.superpowers/sdd/progress.md`.
