# Lumina P1.5 UX Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship Streamlit-class visual polish and app shell (design tokens, page config API, restyled widgets, showcase demo) at `0.5.0-SNAPSHOT`.

**Architecture:** Framework-owned CSS split into token/base/layout/component layers; `Ui.pageConfig()` writes root props consumed by `LuminaApp.render()`; all existing widgets restyled against `--lumina-*` tokens with dark-mode baseline.

**Tech Stack:** Java 25, Maven reactor, vanilla Web Components, CSS custom properties, JUnit 5, existing WebSocket snapshot protocol.

## Global Constraints

- Zero user-authored HTML/CSS/JS — framework owns all styling.
- Java 25+, Spring Boot 4.1.0, Jetty 12.1.11 (inherit parent POM).
- Version target: `0.5.0-SNAPSHOT`.
- `@Transactional` on service methods only; tests via `mvn clean verify`.
- Page config must be first `Ui` call per `build()` or throw `LuminaException`.
- Wire: root props only — no new message types.
- Primary accent: `#2563EB`; sidebar width: `21rem`.

---

## File map

| File | Responsibility |
|------|----------------|
| `lumina-core/.../PageLayout.java` | Layout mode enum |
| `lumina-core/.../SidebarState.java` | Sidebar visual state enum |
| `lumina-core/.../PageConfig.java` | Page config record + builder |
| `lumina-core/.../Ui.java` | Add `pageConfig(PageConfig)` |
| `lumina-components/.../ComponentSpecs.java` | `PAGE_TITLE`, `LAYOUT`, `SIDEBAR_STATE` |
| `lumina-runtime/.../UiBinder.java` | Page config tracking + root props |
| `lumina-web/.../lumina-tokens.css` | Design tokens + dark overrides |
| `lumina-web/.../lumina-base.css` | Reset, body, typography |
| `lumina-web/.../lumina-layout.css` | App shell, sidebar, columns, expander |
| `lumina-web/.../lumina-components.css` | Widget styles |
| `lumina-web/.../lumina.css` | `@import` aggregator |
| `lumina-web/.../lumina-client.js` | `applyPageConfig()` in `LuminaApp.render()` |
| `lumina-examples/.../ShowcaseApp.java` | Hero demo |
| `docs/VISION.md` | P1.5 roadmap + updated matrix |

---

### Task 1: PageConfig API + root props

**Files:**
- Create: `lumina-core/src/main/java/io/lumina/ui/PageLayout.java`
- Create: `lumina-core/src/main/java/io/lumina/ui/SidebarState.java`
- Create: `lumina-core/src/main/java/io/lumina/ui/PageConfig.java`
- Modify: `lumina-core/src/main/java/io/lumina/ui/Ui.java`
- Modify: `lumina-components/src/main/java/io/lumina/components/ComponentSpecs.java`
- Modify: `lumina-core/src/test/java/io/lumina/ui/UiSignatureTest.java`
- Test: `lumina-runtime/src/test/java/io/lumina/runtime/UiBinderPageConfigTest.java`

**Interfaces:**
- Consumes: existing `Ui`, `ComponentNode`, `ComponentTypes.ROOT`
- Produces: `PageConfig`, `PageLayout`, `SidebarState`, `Ui.pageConfig(PageConfig)`, spec keys `PAGE_TITLE`, `LAYOUT`, `SIDEBAR_STATE`

- [ ] **Step 1: Write failing test `UiBinderPageConfigTest`**

```java
package io.lumina.runtime;

import static io.lumina.components.ComponentSpecs.LAYOUT;
import static io.lumina.components.ComponentSpecs.PAGE_TITLE;
import static io.lumina.components.ComponentSpecs.SIDEBAR_STATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumina.LuminaException;
import io.lumina.model.ComponentNode;
import io.lumina.session.internal.SessionState;
import io.lumina.ui.PageConfig;
import io.lumina.ui.PageLayout;
import io.lumina.ui.SidebarState;
import org.junit.jupiter.api.Test;

class UiBinderPageConfigTest {

    @Test
    void pageConfigEmitsRootProps() {
        UiBinder ui = new UiBinder(new SessionState());
        ui.pageConfig(PageConfig.builder()
                .title("Dashboard")
                .layout(PageLayout.CENTERED)
                .sidebar(SidebarState.COLLAPSED)
                .build());
        ui.title("Hello");

        ComponentNode root = ui.buildRoot();
        assertThat(root.props())
                .containsEntry(PAGE_TITLE, "Dashboard")
                .containsEntry(LAYOUT, "centered")
                .containsEntry(SIDEBAR_STATE, "collapsed");
    }

    @Test
    void pageConfigMustBeFirstCall() {
        UiBinder ui = new UiBinder(new SessionState());
        ui.text("too early");
        assertThatThrownBy(() -> ui.pageConfig(PageConfig.builder().build()))
                .isInstanceOf(LuminaException.class)
                .hasMessageContaining("first");
    }
}
```

- [ ] **Step 2: Run test — expect FAIL**

Run: `mvn -q -pl lumina-runtime -am test -Dtest=UiBinderPageConfigTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL (types/methods missing)

- [ ] **Step 3: Implement enums, PageConfig, ComponentSpecs keys, Ui method, UiBinder**

`PageLayout.java`:
```java
package io.lumina.ui;

public enum PageLayout {
    WIDE,
    CENTERED;

    public String wireValue() {
        return name().toLowerCase();
    }
}
```

`SidebarState.java`:
```java
package io.lumina.ui;

public enum SidebarState {
    EXPANDED,
    COLLAPSED;

    public String wireValue() {
        return name().toLowerCase();
    }
}
```

`PageConfig.java` — record + builder per spec §4.2.

`ComponentSpecs.java` — add:
```java
public static final String PAGE_TITLE = "pageTitle";
public static final String LAYOUT = "layout";
public static final String SIDEBAR_STATE = "sidebarState";
```

`Ui.java` — add `void pageConfig(PageConfig config);`

`UiBinder.java`:
```java
private PageConfig pageConfig;
private boolean pageConfigLocked;

@Override
public void pageConfig(PageConfig config) {
    Objects.requireNonNull(config, "config");
    if (pageConfigLocked) {
        throw new LuminaException("pageConfig() must be the first Ui call in build()");
    }
    this.pageConfig = config;
}

private void lockPageConfig() {
    pageConfigLocked = true;
}

// Call lockPageConfig() at start of every other Ui method (title, markdown, container, etc.)

public ComponentNode buildRoot() {
    Map<String, Object> props = new LinkedHashMap<>();
    if (pageConfig != null) {
        if (!pageConfig.title().isBlank()) {
            props.put(PAGE_TITLE, pageConfig.title());
        }
        props.put(LAYOUT, pageConfig.layout().wireValue());
        props.put(SIDEBAR_STATE, pageConfig.sidebarState().wireValue());
    }
    return new ComponentNode("root", ComponentTypes.ROOT, Map.copyOf(props),
            List.copyOf(frames.peek().children()));
}
```

Update `UiSignatureTest.FakeUi` with no-op `pageConfig`.

- [ ] **Step 4: Run tests — expect PASS**

Run: `mvn -q -pl lumina-runtime,lumina-core -am test -Dtest=UiBinderPageConfigTest,UiSignatureTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add lumina-core/src/main/java/io/lumina/ui/PageLayout.java \
        lumina-core/src/main/java/io/lumina/ui/SidebarState.java \
        lumina-core/src/main/java/io/lumina/ui/PageConfig.java \
        lumina-core/src/main/java/io/lumina/ui/Ui.java \
        lumina-components/src/main/java/io/lumina/components/ComponentSpecs.java \
        lumina-core/src/test/java/io/lumina/ui/UiSignatureTest.java \
        lumina-runtime/src/main/java/io/lumina/runtime/UiBinder.java \
        lumina-runtime/src/test/java/io/lumina/runtime/UiBinderPageConfigTest.java
git commit -m "feat: add PageConfig API and root props for app shell"
```

---

### Task 2: Design tokens + base CSS

**Files:**
- Create: `lumina-web/src/main/resources/static/lumina-web/lumina-tokens.css`
- Create: `lumina-web/src/main/resources/static/lumina-web/lumina-base.css`
- Modify: `lumina-web/src/main/resources/static/lumina-web/lumina.css`

**Interfaces:**
- Consumes: spec §7.2 token list
- Produces: `--lumina-*` variables available to all stylesheets

- [ ] **Step 1: Create `lumina-tokens.css`** with full token set from spec §7.2 including `@media (prefers-color-scheme: dark)` overrides for all color tokens.

- [ ] **Step 2: Create `lumina-base.css`**

```css
*, *::before, *::after { box-sizing: border-box; }

body {
  margin: 0;
  font-family: var(--lumina-font-sans);
  font-size: var(--lumina-text-base);
  line-height: 1.5;
  color: var(--lumina-color-text);
  background: var(--lumina-color-bg);
  -webkit-font-smoothing: antialiased;
}
```

- [ ] **Step 3: Replace `lumina.css` body with imports**

```css
@import url("lumina-tokens.css");
@import url("lumina-base.css");
@import url("lumina-layout.css");
@import url("lumina-components.css");
```

Move any still-needed rules from old `lumina.css` into layout/components tasks; leave aggregator only.

- [ ] **Step 4: Verify static resources serve**

Run: `mvn -q -pl lumina-web -am test -Dtest=LuminaServerIT -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS (existing ITs still green)

- [ ] **Step 5: Commit**

```bash
git add lumina-web/src/main/resources/static/lumina-web/lumina-tokens.css \
        lumina-web/src/main/resources/static/lumina-web/lumina-base.css \
        lumina-web/src/main/resources/static/lumina-web/lumina.css
git commit -m "feat: add Lumina design tokens and base CSS"
```

---

### Task 3: App shell CSS + client page config

**Files:**
- Create: `lumina-web/src/main/resources/static/lumina-web/lumina-layout.css` (shell portion)
- Modify: `lumina-web/src/main/resources/static/lumina-web/lumina-client.js`
- Test: `lumina-web/src/test/java/io/lumina/web/ProtocolCodecPageConfigTest.java`
- Modify: `lumina-web/src/test/java/io/lumina/web/LuminaServerIT.java`

**Interfaces:**
- Consumes: root props `pageTitle`, `layout`, `sidebarState`
- Produces: `LuminaApp.applyPageConfig(props)`, layout CSS classes

- [ ] **Step 1: Write failing `ProtocolCodecPageConfigTest`**

```java
@Test
void snapshotIncludesRootPageConfigProps() {
    Map<String, Object> props = Map.of(
            "pageTitle", "Dash",
            "layout", "wide",
            "sidebarState", "expanded");
    ComponentNode root = new ComponentNode("root", "root", props, List.of());
    String json = ProtocolCodec.toSnapshotJson(root);
    assertThat(json).contains("\"pageTitle\":\"Dash\"");
    assertThat(json).contains("\"layout\":\"wide\"");
}
```

- [ ] **Step 2: Run — expect PASS immediately** (serialization already works; test guards regression)

- [ ] **Step 3: Add shell CSS to `lumina-layout.css`**

Key rules:
```css
lumina-app {
  display: block;
  min-height: 100dvh;
  padding: var(--lumina-content-padding);
}

lumina-app.lumina-layout-wide {
  width: 100%;
  max-width: none;
  margin: 0;
}

lumina-app.lumina-layout-centered {
  max-width: var(--lumina-content-max-centered);
  margin: 0 auto;
}

lumina-app.lumina-has-sidebar {
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  gap: var(--lumina-space-6);
  max-width: none;
  width: 100%;
}

lumina-sidebar, .lumina-sidebar {
  flex: 0 0 var(--lumina-sidebar-width);
  background: var(--lumina-color-sidebar);
  border-right: 1px solid var(--lumina-color-border);
  border-radius: var(--lumina-radius-lg);
  padding: var(--lumina-space-4);
  min-height: calc(100dvh - 2 * var(--lumina-content-padding));
}

lumina-app.lumina-sidebar-collapsed lumina-sidebar {
  flex-basis: 0;
  width: 0;
  padding: 0;
  overflow: hidden;
  border: none;
}
```

- [ ] **Step 4: Update `LuminaApp.render()` in `lumina-client.js`**

```javascript
render() {
  this.applyPageConfig(this.tree?.props ?? {});
  const status = this.querySelector(":scope > .lumina-status");
  const fragment = document.createDocumentFragment();
  for (const child of this.tree?.children ?? []) {
    fragment.append(renderNode(child));
  }
  this.replaceChildren(fragment);
  if (status?.textContent) {
    this.append(status);
  }
}

applyPageConfig(props) {
  const layout = props.layout === "centered" ? "centered" : "wide";
  const sidebarState = props.sidebarState === "collapsed" ? "collapsed" : "expanded";
  this.classList.remove("lumina-layout-wide", "lumina-layout-centered",
      "lumina-sidebar-expanded", "lumina-sidebar-collapsed", "lumina-has-sidebar");
  this.classList.add(layout === "centered" ? "lumina-layout-centered" : "lumina-layout-wide");
  this.classList.add(sidebarState === "collapsed" ? "lumina-sidebar-collapsed" : "lumina-sidebar-expanded");
  const hasSidebar = (this.tree?.children ?? []).some((c) => c.type === "sidebar");
  if (hasSidebar) {
    this.classList.add("lumina-has-sidebar");
  }
  if (props.pageTitle) {
    document.title = String(props.pageTitle);
  }
}
```

- [ ] **Step 5: Extend `LuminaServerIT` with page config snapshot test** using inline app calling `ui.pageConfig(...)` first; assert snapshot contains `"pageTitle"`.

- [ ] **Step 6: Run ITs**

Run: `mvn -q -pl lumina-web -am test -Dtest=LuminaServerIT,ProtocolCodecPageConfigTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git commit -m "feat: app shell CSS and client page config application"
```

---

### Task 4: Component stylesheet pass

**Files:**
- Create: `lumina-web/src/main/resources/static/lumina-web/lumina-components.css`
- Modify: `lumina-web/src/main/resources/static/lumina-web/lumina-layout.css` (layout primitives beyond shell)

**Interfaces:**
- Consumes: `--lumina-*` tokens
- Produces: styled rules for all 22 `ComponentTypes` + status line

- [ ] **Step 1: Move layout rules** (columns, column, container, expander) into `lumina-layout.css` using tokens — expander as card-style panel:

```css
lumina-expander details {
  border: 1px solid var(--lumina-color-border);
  border-radius: var(--lumina-radius-lg);
  background: var(--lumina-color-bg-subtle);
  box-shadow: var(--lumina-shadow-sm);
  padding: var(--lumina-space-2) var(--lumina-space-4);
}
```

- [ ] **Step 2: Write `lumina-components.css`** covering all widget types per spec §7.4 checklist. Primary button example:

```css
lumina-button button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 2.5rem;
  padding: 0 var(--lumina-space-4);
  border: none;
  border-radius: var(--lumina-radius-md);
  background: var(--lumina-color-primary);
  color: #FFFFFF;
  font: inherit;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s ease;
}
lumina-button button:hover {
  background: var(--lumina-color-primary-hover);
}
lumina-button button:focus-visible {
  outline: 2px solid var(--lumina-color-primary);
  outline-offset: 2px;
}
```

Include chat bubbles, inputs, tables, code blocks, markdown headings, progress, title hero sizing.

- [ ] **Step 3: Remove duplicated rules from old inline `lumina.css`** (should only import).

- [ ] **Step 4: Run full web tests**

Run: `mvn -q -pl lumina-web -am test -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git commit -m "feat: restyle all widgets and layout primitives with design tokens"
```

---

### Task 5: ShowcaseApp + documentation

**Files:**
- Create: `lumina-examples/src/main/java/io/lumina/examples/showcase/ShowcaseApp.java`
- Create: `lumina-examples/src/main/java/io/lumina/examples/showcase/ShowcaseMain.java`
- Modify: `lumina-examples/README.md`
- Modify: `README.md` (add Showcase quickstart section)

**Interfaces:**
- Consumes: `PageConfig`, all layout + widget APIs
- Produces: runnable hero demo entry point

- [ ] **Step 1: Implement `ShowcaseApp`** per spec §8 (pageConfig, sidebar, title, columns, expander, textInput, button).

- [ ] **Step 2: Implement `ShowcaseMain`** mirroring `LayoutDemoMain` pattern.

- [ ] **Step 3: Update README files** — Showcase as recommended first demo; document run command:

```bash
mvn -q -pl lumina-examples exec:java -Dexec.mainClass=io.lumina.examples.showcase.ShowcaseMain
```

- [ ] **Step 4: Manual smoke test** — run ShowcaseMain, verify wide layout + sidebar + styled widgets in browser.

- [ ] **Step 5: Commit**

```bash
git commit -m "feat: add ShowcaseApp hero demo for P1.5 UX"
```

---

### Task 6: Roadmap docs + version bump + verify

**Files:**
- Modify: `docs/VISION.md`
- Modify: `docs/adr/ADR-013-design-system-ownership.md` (already created — verify status line)
- Modify: all `pom.xml` version → `0.5.0-SNAPSHOT`

- [ ] **Step 1: Update `docs/VISION.md`**
  - Insert P1.5 in roadmap list
  - Fix stale matrix rows (nested layout ✅, basic layout ✅, add P1.5 rows for design system / app shell / showcase)
  - Update resume point: P1.5 → P1 gaps (routing, hot reload, SSE) → P2 widgets

- [ ] **Step 2: Bump version**

Run: `mvn -q versions:set -DnewVersion=0.5.0-SNAPSHOT -DgenerateBackupPoms=false`

- [ ] **Step 3: Full verify**

Run: `mvn -q clean verify`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git commit -m "chore: bump to 0.5.0-SNAPSHOT and update roadmap for P1.5 UX"
```

---

## Plan self-review

| Spec requirement | Task |
|------------------|------|
| PageConfig API | Task 1 |
| Root props wire | Task 1, 3 |
| CSS tokens + dark mode | Task 2 |
| App shell wide/centered | Task 3 |
| All widgets styled | Task 4 |
| ShowcaseApp | Task 5 |
| VISION + ADR-013 | Task 6 |
| 0.5.0-SNAPSHOT | Task 6 |
| P2 gate documented | VISION.md Task 6 |

No placeholders remain. Types consistent across tasks.

---

## Execution handoff

Plan complete and saved to `docs/superpowers/plans/2026-07-25-lumina-p15-ux-foundation.md`.

**Two execution options:**

1. **Subagent-Driven (recommended)** — fresh subagent per task, review between tasks
2. **Inline Execution** — implement tasks in this session with checkpoints

Which approach?
