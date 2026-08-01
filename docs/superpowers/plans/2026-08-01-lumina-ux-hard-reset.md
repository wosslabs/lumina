# Lumina UX Hard Reset (W0→W2) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild Lumina’s thin-client chrome and shipped widgets to an enterprise product-shell UX bar at `0.7.0-SNAPSHOT`, per `docs/superpowers/specs/2026-08-01-lumina-ux-hard-reset-design.md`.

**Architecture:** Keep the Java rerun/diff/WebSocket/routing kernel. Add first-class shell tree nodes (`sidebar_brand`, `sidebar_nav`, `nav_page`, `sidebar_footer`, `app_header`) and `SidebarUi`/`NavUi`/`HeaderUi` author APIs. Replace tokens/CSS and client rendering for landmarks, banner, nav (`aria-current` from root `path`), and all existing widgets. Defer new P2 widgets (checkbox/select/…).

**Tech Stack:** Java 25, Maven reactor, vanilla Web Components, CSS custom properties, JUnit 5, existing WebSocket protocol.

## Global Constraints

- Spec: `docs/superpowers/specs/2026-08-01-lumina-ux-hard-reset-design.md`
- Version target: `0.7.0-SNAPSHOT`
- Zero user-authored HTML/CSS/JS
- Routing intents unchanged (`connect`+path, `navigate`, `click`, `input`)
- Client derives `aria-current` by comparing `nav_page.path` to root `path`
- Retire Streamlit-red `#FF4B4B` as default brand; neutral enterprise shell + restrained accent (e.g. `#2563EB`)
- `mvn clean verify` must pass after each task
- Work on branch `feature/lumina-ux-hard-reset` (not main)
- Commits allowed per task; no push unless asked

## File map

| File | Responsibility |
|------|----------------|
| `docs/superpowers/specs/2026-08-01-lumina-ux-constitution-checklist.md` | W0 PR checklist |
| `lumina-core/.../SidebarUi.java` | Sidebar slots API |
| `lumina-core/.../NavUi.java` | `page(label, path)` |
| `lumina-core/.../HeaderUi.java` | Header title API |
| `lumina-core/.../Ui.java` | `sidebar(Consumer<SidebarUi>)`, `header(Consumer<HeaderUi>)` |
| `lumina-core/.../ComponentTypes.java` | New shell type constants |
| `lumina-components/.../ComponentSpecs.java` | Any new prop keys if needed |
| `lumina-runtime/.../UiBinder.java` | Shell binding + legacy freeform sidebar |
| `lumina-web/.../lumina-tokens.css` | Enterprise tokens |
| `lumina-web/.../lumina-base.css` | Base + reduced-motion |
| `lumina-web/.../lumina-layout.css` | Banner, sidebar regions, landmarks |
| `lumina-web/.../lumina-components.css` | Widget rebuild |
| `lumina-web/.../lumina-client.js` | Banner, shell elements, nav navigate, a11y |
| `lumina-examples/.../ShowcaseApp.java` | Migrate to brand/nav/footer + header |
| `docs/VISION.md` | Roadmap status |
| all `pom.xml` | `0.7.0-SNAPSHOT` |

---

### Task 1: W0 constitution checklist

**Files:**
- Create: `docs/superpowers/specs/2026-08-01-lumina-ux-constitution-checklist.md`

**Interfaces:**
- Consumes: design spec §4
- Produces: PR checklist used by later tasks

- [ ] **Step 1: Write checklist** covering semantics, keyboard/focus, density (≥40px), quiet chrome, motion/`prefers-reduced-motion`, forms/labels, WCAG AA, thin-client invariant — each as a checkbox item.

- [ ] **Step 2: Commit**

```bash
git add docs/superpowers/specs/2026-08-01-lumina-ux-constitution-checklist.md
git commit -m "docs: add UX constitution PR checklist for hard-reset epic"
```

---

### Task 2: Shell API types + Ui signatures

**Files:**
- Create: `lumina-core/src/main/java/io/lumina/ui/NavUi.java`
- Create: `lumina-core/src/main/java/io/lumina/ui/SidebarUi.java`
- Create: `lumina-core/src/main/java/io/lumina/ui/HeaderUi.java`
- Modify: `lumina-core/src/main/java/io/lumina/ui/Ui.java`
- Modify: `lumina-core/src/main/java/io/lumina/model/ComponentTypes.java`
- Modify: `lumina-core/src/test/java/io/lumina/ui/UiSignatureTest.java`
- Modify: any other `implements Ui` stubs that fail compile

**Interfaces:**
- Produces:
  - `NavUi.page(String label, String path)`
  - `SidebarUi extends Ui` with `brand(Consumer<Ui>)`, `nav(Consumer<NavUi>)`, `footer(Consumer<Ui>)`
  - `HeaderUi.title(String text)`
  - `Ui.sidebar(Consumer<SidebarUi> body)` (replace `Consumer<Ui>`)
  - `Ui.header(Consumer<HeaderUi> body)`
  - Types: `SIDEBAR_BRAND`, `SIDEBAR_NAV`, `NAV_PAGE`, `SIDEBAR_FOOTER`, `APP_HEADER`

- [ ] **Step 1: Add component type constants**

```java
public static final String SIDEBAR_BRAND = "sidebar_brand";
public static final String SIDEBAR_NAV = "sidebar_nav";
public static final String NAV_PAGE = "nav_page";
public static final String SIDEBAR_FOOTER = "sidebar_footer";
public static final String APP_HEADER = "app_header";
```

- [ ] **Step 2: Add NavUi / SidebarUi / HeaderUi and update Ui**

```java
public interface NavUi {
    void page(String label, String path);
}

public interface SidebarUi extends Ui {
    void brand(Consumer<Ui> body);
    void nav(Consumer<NavUi> body);
    void footer(Consumer<Ui> body);
}

public interface HeaderUi {
    void title(String text);
}
```

Change `void sidebar(Consumer<Ui> body)` → `void sidebar(Consumer<SidebarUi> body)` and add `void header(Consumer<HeaderUi> body)`.

- [ ] **Step 3: Update FakeUi and compile**

Run: `mvn -q -pl lumina-core,lumina-runtime,lumina-examples -am compile`
Expected: FAIL until UiBinder updated — or add temporary stubs in UiBinder that throw UnsupportedOperationException for new methods if compile of runtime is in same reactor. Prefer implementing stubs in Task 3 immediately after; for Task 2 only `lumina-core` must compile:

Run: `mvn -q -pl lumina-core test`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git commit -m "feat: add SidebarUi/NavUi/HeaderUi and shell component types"
```

---

### Task 3: UiBinder shell binding + unit tests

**Files:**
- Modify: `lumina-runtime/src/main/java/io/lumina/runtime/UiBinder.java`
- Create: `lumina-runtime/src/test/java/io/lumina/runtime/UiBinderShellTest.java`
- Modify: `ColumnScopedUi` to implement new methods by delegating or throwing unsupported for header/sidebar-only APIs as appropriate

**Interfaces:**
- Consumes: Task 2 APIs and types
- Produces: tree shape

```
root
├── app_header? (props: title)
├── sidebar
│   ├── sidebar_brand? (children)
│   ├── sidebar_nav? (children: nav_page{label,path}*)
│   ├── sidebar_footer? (children)
│   └── …legacy freeform children…
└── …main widgets…
```

- [ ] **Step 1: Write failing `UiBinderShellTest`**

```java
@Test
void structuredSidebarEmitsBrandNavFooterAndNavPages() {
    UiBinder ui = new UiBinder(new SessionState());
    ui.pageConfig(PageConfig.builder().title("App").build());
    ui.sidebar(sb -> {
        sb.brand(b -> b.text("Brand"));
        sb.nav(nav -> {
            nav.page("Home", "/");
            nav.page("About", "/about");
        });
        sb.footer(f -> f.text("Foot"));
    });
    ui.header(h -> h.title("Context"));
    ui.title("Page");

    ComponentNode root = ui.buildRoot();
    assertThat(root.children()).anyMatch(n -> "app_header".equals(n.type()));
    ComponentNode sidebar = root.children().stream().filter(n -> "sidebar".equals(n.type())).findFirst().orElseThrow();
    assertThat(sidebar.children()).extracting(ComponentNode::type)
            .contains("sidebar_brand", "sidebar_nav", "sidebar_footer");
    ComponentNode nav = sidebar.children().stream().filter(n -> "sidebar_nav".equals(n.type())).findFirst().orElseThrow();
    assertThat(nav.children()).allMatch(n -> "nav_page".equals(n.type()));
    assertThat(nav.children().get(0).props()).containsEntry("label", "Home").containsEntry("path", "/");
}

@Test
void legacyFreeformSidebarStillWorks() {
    UiBinder ui = new UiBinder(new SessionState());
    ui.sidebar(sb -> sb.button("Only"));
    ComponentNode sidebar = ui.buildRoot().children().getFirst();
    assertThat(sidebar.type()).isEqualTo("sidebar");
    assertThat(sidebar.children().getFirst().type()).isEqualTo("button");
}
```

- [ ] **Step 2: Run test — expect FAIL**

Run: `mvn -q -pl lumina-runtime -Dtest=UiBinderShellTest test`

- [ ] **Step 3: Implement SidebarScopedUi / Header binding in UiBinder**

- `sidebar(Consumer<SidebarUi>)`: one sidebar per build; provide `SidebarScopedUi` that extends/implements `SidebarUi` and delegates widget methods to parent with sidebar frame as target.
- `brand`/`footer`: `withLayoutFrame` for `SIDEBAR_BRAND` / `SIDEBAR_FOOTER`.
- `nav`: frame `SIDEBAR_NAV`; `NavUi.page` adds `NAV_PAGE` node with `Map.of(LABEL, label, PATH, SessionRoutes.normalize(path))` — reuse `ComponentSpecs.PATH` and `LABEL`.
- Blank path → `LuminaException`.
- `header`: at most once; adds `APP_HEADER` with prop title (use `CONTENT` or add `HEADER_TITLE` — prefer `CONTENT` for the context string to avoid new keys, OR `LABEL` — **use `CONTENT`** for header title text).
- ColumnScopedUi: `sidebar`/`header` throw `LuminaException("sidebar/header not allowed in columns")` or delegate to parent — **delegate to parent** for header; sidebar still once-per-build on parent.

- [ ] **Step 4: Run tests PASS**

Run: `mvn -q -pl lumina-runtime -Dtest=UiBinderShellTest,UiBinderTest,UiBinderRoutingTest,UiBinderPageConfigTest test`

- [ ] **Step 5: Commit**

```bash
git commit -m "feat: bind structured sidebar slots and app header in UiBinder"
```

---

### Task 4: Client shell — banner, landmarks, nav_page

**Files:**
- Modify: `lumina-web/src/main/resources/static/lumina-web/lumina-client.js`
- Modify: `lumina-web/src/test/java/io/lumina/web/LuminaServerIT.java` (shell snapshot + nav click)

**Interfaces:**
- Consumes: shell node types; root `path` / `pageTitle`
- Produces: DOM landmarks; `navigate` intent from nav clicks

- [ ] **Step 1: Register elements** in `ELEMENTS`:

```javascript
sidebar_brand: "lumina-sidebar-brand",
sidebar_nav: "lumina-sidebar-nav",
nav_page: "lumina-nav-page",
sidebar_footer: "lumina-sidebar-footer",
app_header: "lumina-app-header"
```

- [ ] **Step 2: Implement elements**

- `lumina-nav-page`: `<button type="button">` or `<a href>` with `preventDefault`; on click `sendIntent("navigate", null, { path })`. Set `aria-current="page"` when `path ===` root path from `closest('lumina-app').tree.props.path`.
- `lumina-sidebar-nav`: role=`navigation` + `aria-label="Primary"`, render children.
- `lumina-sidebar`: role=`complementary` (or keep host + inner).
- `lumina-app-header`: render context title as `<p class="lumina-header-title">` (not H1).
- `LuminaApp.render()`:
  - Prepend `<header class="lumina-banner" role="banner">` with product name from `pageTitle` (fallback "Lumina").
  - Sidebar then `<main class="lumina-main">` for non-sidebar, non-header children; place `app_header` inside main top or between banner and main per spec — **place app_header as first child inside main**.

- [ ] **Step 3: IT — snapshot contains shell types; nav click changes path**

Extend `LuminaServerIT` with app using structured sidebar + assert `"sidebar_nav"` / `"nav_page"` in snapshot; send navigate or click nav via intent `navigate` with `/about` and assert path.

- [ ] **Step 4: Run** `mvn -q -pl lumina-web -Dtest=LuminaServerIT test` — PASS

- [ ] **Step 5: Commit**

```bash
git commit -m "feat: client enterprise shell banner, landmarks, and nav_page"
```

---

### Task 5: Enterprise tokens + layout CSS

**Files:**
- Modify: `lumina-web/src/main/resources/static/lumina-web/lumina-tokens.css`
- Modify: `lumina-web/src/main/resources/static/lumina-web/lumina-base.css`
- Modify: `lumina-web/src/main/resources/static/lumina-web/lumina-layout.css`

**Interfaces:**
- Produces: neutral enterprise palette; banner/sidebar/nav styles; `prefers-reduced-motion`

- [ ] **Step 1: Replace tokens** — primary `#2563EB` / hover `#1D4ED8`; neutral bg/sidebar/border/text; keep dark scheme overrides AA-capable.

- [ ] **Step 2: Base** — font stack enterprise (keep Inter/system); add:

```css
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    animation-duration: 0.01ms !important;
    transition-duration: 0.01ms !important;
  }
}
```

- [ ] **Step 3: Layout** — `.lumina-banner` sticky/top bar; sidebar brand/nav/footer zones; `lumina-nav-page button` full-width, min-height 2.5rem, `[aria-current="page"]` accent; remove Streamlit-red assumptions.

- [ ] **Step 4: Commit**

```bash
git commit -m "style: enterprise tokens and shell layout chrome"
```

---

### Task 6: Widget CSS + client a11y pass (W2)

**Files:**
- Modify: `lumina-web/src/main/resources/static/lumina-web/lumina-components.css`
- Modify: `lumina-web/src/main/resources/static/lumina-web/lumina-client.js` (labels, table scope, chat aria, expander already fixed)

**Interfaces:**
- Consumes: constitution checklist
- Produces: rebuilt widgets meeting W0

- [ ] **Step 1: Components CSS** — denser controls, ≥40px primary buttons/inputs min-height, consistent focus-visible on all interactive widgets, quieter containers, table header styling.

- [ ] **Step 2: Client a11y**
  - text_input / file_upload: proper `<label for>` + unique input ids from node id
  - table: `<th scope="col">`
  - ai_message streaming: `aria-live="polite"` on message container
  - user_message: no live region
  - button min hit target via CSS

- [ ] **Step 3: Run** `mvn -q -pl lumina-web,lumina-examples -am test` — PASS

- [ ] **Step 4: Commit**

```bash
git commit -m "style: rebuild widget CSS and tighten client accessibility"
```

---

### Task 7: Showcase + examples migration

**Files:**
- Modify: `lumina-examples/src/main/java/io/lumina/examples/showcase/ShowcaseApp.java`
- Modify: `lumina-examples/src/main/java/io/lumina/examples/layout/LayoutDemoApp.java` (if sidebar used)
- Modify: any compile breaks from `Consumer<SidebarUi>`

**Interfaces:**
- Consumes: shell APIs

- [ ] **Step 1: Rewrite ShowcaseApp sidebar** to `brand` / `nav.page` / `footer`; add `ui.header` on pages; keep Home/About routing demo.

- [ ] **Step 2: Fix LayoutDemoApp** similarly if needed.

- [ ] **Step 3: Run** `mvn -q -pl lumina-examples -am test` — PASS

- [ ] **Step 4: Commit**

```bash
git commit -m "feat: migrate ShowcaseApp to enterprise shell navigation"
```

---

### Task 8: Version bump + VISION

**Files:**
- Modify: all module `pom.xml` → `0.7.0-SNAPSHOT`
- Modify: `docs/VISION.md`
- Modify: plan/spec status notes if needed

- [ ] **Step 1:** `mvn -q versions:set -DnewVersion=0.7.0-SNAPSHOT -DgenerateBackupPoms=false`

- [ ] **Step 2: Update VISION** — note UX hard-reset (`0.7.0`); P1.5 marked superseded visually; resume P2 widgets after this; hot reload/SSE still pending.

- [ ] **Step 3:** `mvn -q clean verify` — PASS

- [ ] **Step 4: Commit**

```bash
git commit -m "chore: bump to 0.7.0-SNAPSHOT and update roadmap for UX hard-reset"
```

---

## Spec coverage self-check

| Spec requirement | Task |
|------------------|------|
| W0 constitution checklist | 1 |
| Sidebar brand/nav/footer APIs | 2–3 |
| Header API | 2–3 |
| nav_page + client aria-current / navigate | 3–4 |
| Banner landmarks | 4–5 |
| Enterprise tokens, reduced motion | 5 |
| Rebuild shipped widgets | 6 |
| Showcase migration | 7 |
| 0.7.0 + VISION | 8 |
| P2 widgets deferred | (explicit non-goal) |

## Execution

User requested: implement the full plan without further approval gates. Use subagent-driven-development: one implementer per task, continuous execution, finish with branch completion options only at the end if required by finishing skill — user already asked for full completion; prefer merge readiness on the feature branch.
