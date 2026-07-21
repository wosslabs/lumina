# Lumina — Nested layout & composition (P1)

**Status:** Approved (design dialogue 2026-07-20)
**Depends on:** Phase 0 (`2026-07-19-lumina-phase0-vision-architecture-design.md`), ADR-007
**Version target:** `0.4.0-SNAPSHOT`
**Java:** 25+ (inherits Phase 0 platform)

## 1. Goal

Evolve Lumina from a **flat** root child list into a **nested** `ComponentNode` tree and ship
Streamlit-basic layout primitives so authors can compose real UIs in pure Java:

- `container`
- `columns(n)` (equal-width columns)
- `sidebar` (one per `build()`)
- `expander` (open/closed persisted for the session)

This closes the largest Phase 1 kernel gap (ADR-007) without changing the server-driven diff +
thin-client contract.

### Canonical example

```java
public final class LayoutDemoApp implements LuminaApp {
    @Override
    public void build(Ui ui) {
        ui.sidebar(nav -> {
            nav.markdown("## Nav");
            if (nav.button("Refresh")) { /* rerun */ }
        });
        ui.columns(2, cols -> {
            cols[0].markdown("### Left");
            cols[1].container(inner -> inner.text("Nested content"));
        });
        ui.expander("Details", body -> body.code("java", "System.out.println();"));
    }
}
```

## 2. Decisions locked in design dialogue

| Topic | Choice |
|-------|--------|
| Scope | Streamlit-basic: `container`, `columns`, `sidebar`, `expander` (not tabs/form/weighted columns) |
| Authoring API | Block callbacks on `Ui` (Streamlit-like) |
| Column widths | Equal only: `columns(int n, Consumer<Ui[]> cols)`, `n >= 1` |
| Expander state | Session-scoped **widget state** (`WidgetState`), keyed by expander node id; default closed; toggle via intent |
| Binder approach | Child-list stack on `UiBinder` (push path + list, run block, pop, attach) — **recommended approach A** |
| `withKey` | Unchanged: key-path scoping only; does **not** create a DOM container |
| Sidebar | At most one `sidebar(...)` per `build()`; second call throws `LuminaException` |
| Diff / wire | Reuse existing nested patch paths and snapshot format (additive intent only) |
| Tabs / form / weighted columns | Deferred |

## 3. Current state (baseline)

| Layer | Today |
|-------|--------|
| `ComponentNode` | Record supports `List<ComponentNode> children` on every node |
| `UiBinder` | All widgets append to a single root `children` list; leaves use `List.of()` |
| `TreeDiffer` | Already diffs nested trees recursively (`/children/0/children/1` paths) |
| Client | `renderNode` recurses; unknown types fall back to generic `div`; patch `addAt` walks nested paths |
| Layout DSL | None |

Implementation focus: **UiBinder stack + new types + client elements/CSS + expander intent**.

## 4. Public API (`lumina-core` — `Ui`)

All additions are additive (new methods on `Ui`; existing methods unchanged).

```java
package io.lumina.ui;

import java.util.function.Consumer;

public interface Ui {
    // ... existing methods ...

    /** Groups widgets in a generic block container. */
    void container(Consumer<Ui> body);

    /**
     * Lays out {@code n} equal-width columns. {@code cols[i]} is the {@code Ui} scoped to column
     * {@code i}. {@code n} must be {@code >= 1}.
     */
    void columns(int n, Consumer<Ui[]> cols);

    /**
     * Renders a left sidebar rail (at most once per {@code build()}). Widgets declared in
     * {@code body} appear in the sidebar.
     */
    void sidebar(Consumer<Ui> body);

    /**
     * Collapsible section with {@code label}. Returns whether the expander is open after this run
     * (including any toggle intent applied before the rebuild). Open/closed persists in session
     * widget state keyed by the expander's node id.
     */
    boolean expander(String label, Consumer<Ui> body);

    // withKey unchanged — path scoping only
}
```

**Column scoping:** `columns(n, cols -> …)` creates one `columns` node with `n` child `column`
nodes. Each `cols[i]` is a binder view whose `addNode` targets that column's child list.

**Sidebar placement:** The `sidebar` node is appended to whichever child list is active (typically
root). Client CSS detects a `sidebar` sibling and lays out the app shell (sidebar + main). Only
one sidebar node per tree is allowed.

## 5. Component model & keying

### 5.1 New `ComponentTypes`

| Constant | Wire `type` | Role |
|----------|-------------|------|
| `CONTAINER` | `container` | Generic grouping |
| `COLUMNS` | `columns` | Row of columns; props: `count` (int) |
| `COLUMN` | `column` | Single column slot; props: `index` (int) |
| `SIDEBAR` | `sidebar` | Left navigation rail |
| `EXPANDER` | `expander` | Collapsible section; props: `label` (string), `open` (boolean) |

Tree shape examples:

```
root
├── sidebar
│   └── …widgets…
├── columns
│   ├── column (index=0)
│   │   └── markdown
│   └── column (index=1)
│       └── button
└── expander (open=true)
    └── code
```

### 5.2 Keying (extends ADR-004)

Auto keys remain `path/type#index` within the current path scope. Layout nodes receive keys via
`nextKey(type)` before pushing a child-list frame. Column bodies push an additional path segment
for the `column` node so widgets inside column 0 vs 1 do not collide.

Explicit keys from `withKey("profile", …)` still prepend `auto:/profile` to the path stack and
do **not** introduce a container node.

## 6. Runtime — `UiBinder` child-list stack

### 6.1 Stack frames

Replace the single root `List<ComponentNode> children` with a stack of frames:

```java
record Frame(String pathSegment, List<ComponentNode> children) {}
Deque<Frame> frames;
```

Root frame is pushed in the constructor (equivalent to today's flat list). `addNode` appends to
`frames.peek().children`.

### 6.2 Layout algorithm (all layout methods)

1. Validate preconditions (e.g. sidebar count, `n >= 1`).
2. Allocate layout node id: `String id = nextKey(type)`.
3. For `columns`: create empty `column` child lists; push each column frame; invoke
   `cols[i]` blocks; pop; assemble `column` nodes as children of `columns`.
4. For `container` / `sidebar` / `expander`: push one frame; run `body`; pop; attach children.
5. Append the layout node to the parent frame's list.

`buildRoot()` reads `frames.peek()` at the root level (only root frame remains) and returns
`new ComponentNode("root", ROOT, Map.of(), rootChildren)`.

### 6.3 Expander state

- On `expander(label, body)`:
  - `String key = nextKey(EXPANDER)` (before pushing body frame).
  - Read `boolean open = Boolean.TRUE.equals(session.widgets().value(key))` (default `false`).
  - Push body frame; run `body`; pop.
  - Emit `ComponentNode(key, EXPANDER, Map.of(LABEL, label, OPEN, open), bodyChildren)`.
  - Return `open`.

- On intent `expander_toggle` with `targetId = key`:
  - `AppRunner.applyIntent` toggles: `widgets.set(key, !Boolean.TRUE.equals(widgets.value(key)))`.
  - Next rerun rebuilds with updated `open` prop → client receives `UPDATE_PROPS` or structural
    patch as usual.

**Note:** Expander uses **`WidgetState`** (same session store as buttons/inputs), not
`ui.state()` / `StateStore`. Keys are the expander's node ids.

### 6.4 Sidebar guard

Track `boolean sidebarUsed` for the current bind. Second `sidebar(...)` throws
`LuminaException("Only one sidebar is allowed per build()")`.

### 6.5 Streaming compatibility

Streaming `ai(TokenStream)` must append to the **current frame's** child list (not hard-coded
root `children`). Interim flush / stream frame behavior is unchanged.

## 7. Wire protocol

Snapshot and patch messages are unchanged (nested paths already supported).

**New client → server intent** (additive):

```json
{
  "type": "intent",
  "name": "expander_toggle",
  "targetId": "<expanderNodeId>"
}
```

Parsed by existing `ProtocolCodec.parseIntent` into
`new Intent("expander_toggle", targetId, Map.of())`.

`AppRunner.applyIntent` gains case `"expander_toggle"` (requires `targetId`).

No new server → client message types.

## 8. Client (`lumina-web`)

### 8.1 Custom elements / render map

Register layout types in the client element map (alongside existing widgets):

| `type` | Element | Notes |
|--------|---------|-------|
| `container` | `<div class="lumina-container">` | Block wrapper |
| `columns` | `<div class="lumina-columns">` | `display: flex; flex-direction: row` |
| `column` | `<div class="lumina-column">` | `flex: 1` |
| `sidebar` | `<aside class="lumina-sidebar">` | Left rail |
| `expander` | `<details class="lumina-expander">` + `<summary>` | `open` attribute from props; toggle sends intent |

`renderNode` recursion is unchanged — layout nodes render children inside the created element.

### 8.2 CSS (`lumina.css`)

- `.lumina-columns` / `.lumina-column` — equal flex columns.
- App shell: when root has a `sidebar` child, use CSS grid or flex on `#app` (or equivalent root)
  so sidebar + remaining siblings form main content area.
- `.lumina-expander` — minimal styling consistent with existing Lumina theme.

### 8.3 Expander toggle

On `<details>` `toggle` event (or click handler): if user toggled, send WebSocket intent
`expander_toggle` with `targetId` = node id. Server reruns; `open` prop updates. Avoid fighting
the user during patch apply: after `UPDATE_PROPS` on expander, sync DOM `open` from props.

## 9. Module changes

| Module | Change |
|--------|--------|
| `lumina-core` | `ComponentTypes` layout constants; `Ui` layout methods |
| `lumina-runtime` | `UiBinder` frame stack + layout methods; `AppRunner` expander intent |
| `lumina-web` | `ProtocolCodec` (no change if generic parse suffices); client JS + CSS |
| `lumina-examples` | `LayoutDemoApp` + main |
| root `pom.xml` | Version `0.4.0-SNAPSHOT` |
| `docs/adr/ADR-007` | Optional status note that implementation began in 0.4 (no rewrite) |

No changes to `TreeDiffer`, `lumina-session` API surface (WidgetState already sufficient), or
Spring modules.

## 10. Testing strategy

| Layer | Test |
|-------|------|
| `UiBinder` | Widgets land under container/column not root; `columns(2)` tree shape; nested `withKey` keys; second `sidebar()` throws; expander reads/writes `WidgetState` |
| `TreeDiffer` | Nested layout ADD/REMOVE/UPDATE paths (extend synthetic container tests) |
| `Ui` API | Signature / compile test for new methods |
| `AppRunner` | `expander_toggle` flips state and subsequent run sets `open` prop |
| `ProtocolCodec` | Parses `expander_toggle` intent |
| `lumina-web` IT | Snapshot contains nested `columns`/`column`; expander toggle updates UI |
| Example smoke | `LayoutDemoApp` builds without error |

## 11. Compatibility & versioning

- Bump to `0.4.0-SNAPSHOT`; MINOR (additive API + intent).
- Wire protocol: additive intent name only; existing clients ignore unknown types gracefully.
- Pre-1.0: nested trees change author-visible structure but not existing flat apps (they keep
  working — root children unchanged semantically).

## 12. Non-goals (deferred)

- Weighted columns, `tabs`, `form`, `dialog`
- Multiple sidebars or sidebar nested inside columns (explicitly disallowed)
- Routing / typed app state (ADR-008)
- Hot reload, SSE, reconnect/resume
- Full accessibility audit (P6)

## 13. ADRs

| ID | Action |
|----|--------|
| ADR-007 | Implementation of this spec; optional one-line status update |
| ADR-003, ADR-004 | Reused by reference (diff + keying) |

## 14. Deliverables checklist

- [ ] `Ui` layout methods + `ComponentTypes`
- [ ] `UiBinder` child-list stack + layout emission + sidebar guard
- [ ] `AppRunner` `expander_toggle` handling
- [ ] Client layout elements + CSS + expander intent
- [ ] `LayoutDemoApp` example
- [ ] Unit + integration tests (§10)
- [ ] Version bump to `0.4.0-SNAPSHOT`
- [ ] Javadoc on all new public API

## 15. Next step

After approval, produce a detailed implementation plan (`docs/superpowers/plans/…`) via the
writing-plans skill, then execute with subagent-driven development.
