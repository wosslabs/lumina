# Lumina — P1.5 UX & Design System Foundation

**Status:** Approved (design dialogue 2026-07-25)
**Depends on:** Nested layout (`2026-07-20-lumina-nested-layout-design.md`, `0.4.0-SNAPSHOT`)
**Version target:** `0.5.0-SNAPSHOT`
**Java:** 25+ (inherits Phase 0 platform)

## 1. Goal

Ship a **Streamlit-class out-of-the-box experience**: professional visual polish and a proper app
shell on first run. Authors still write zero HTML/CSS/JS; the framework owns the design system,
layout modes, and showcase demo.

This phase closes the gap between "layout primitives work" and "product looks enterprise-grade."

### Success criteria

- `ShowcaseApp` opened in a browser looks credible next to Streamlit (typography, spacing, shell,
  widget polish, dark mode baseline).
- Every **existing** widget and layout primitive is restyled against shared tokens.
- New P2 widgets must not ship until they meet the P1.5 design checklist (documented in plan).
- `ui.pageConfig(...)` provides Streamlit `set_page_config` parity for title and layout mode.

## 2. Decisions locked in design dialogue

| Topic | Choice |
|-------|--------|
| Scope | Visual polish **and** layout/shell together (option C) |
| Author CSS | Forbidden — framework-owned CSS only (hard invariant unchanged) |
| CSS delivery | Split source files (`tokens`, `base`, `components`, `layout`); `lumina.css` aggregates via `@import` (single browser request) |
| Typography | System stack first: `"Inter", system-ui, sans-serif`; optional bundled `@font-face` if license/size acceptable |
| Brand accent | Lumina primary (`#2563EB` blue) — distinct from Streamlit red, equally professional |
| Layout modes | `WIDE` (default) and `CENTERED` |
| Sidebar width | `21rem` styled rail; `EXPANDED` / `COLLAPSED` visual state (no animation in P1.5) |
| Page config wire | Root node `props` on snapshot — no new message types |
| Page config rule | Must be first `Ui` call in `build()`; late call throws `LuminaException` |
| Dark mode | `prefers-color-scheme: dark` token overrides; manual toggle deferred to P6 |
| Demo app | New `ShowcaseApp` as README hero; keep `LayoutDemoApp` for minimal layout IT |
| Theme SPI | Internal token contract only; public theme SDK remains P8 |
| Deferred | Tabs, dialogs, toasts, sidebar collapse animation, author themes |

## 3. Current state (baseline)

| Layer | Today |
|-------|--------|
| CSS | Single `lumina.css` (~227 lines), system colors, `44rem` narrow column |
| App shell | Minimal flex when sidebar present; no visual rail styling |
| Page config | None — static `<title>Lumina</title>` in `index.html` |
| Widgets | Functional but browser-default appearance |
| Layout demo | `LayoutDemoApp` — structural skeleton only |
| Root props | Always `Map.of()` on `buildRoot()` |

## 4. Public API (`lumina-core`)

### 4.1 Enums

```java
package io.lumina.ui;

/** Main content width mode (Streamlit wide vs centered). */
public enum PageLayout {
    WIDE,
    CENTERED
}

/** Sidebar visual state for the current page. */
public enum SidebarState {
    EXPANDED,
    COLLAPSED
}
```

### 4.2 `PageConfig`

```java
package io.lumina.ui;

/** Streamlit {@code set_page_config} equivalent — must be the first {@link Ui} call per build. */
public record PageConfig(String title, PageLayout layout, SidebarState sidebarState) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String title = "";
        private PageLayout layout = PageLayout.WIDE;
        private SidebarState sidebarState = SidebarState.EXPANDED;

        public Builder title(String title) { this.title = title; return this; }
        public Builder layout(PageLayout layout) { this.layout = layout; return this; }
        public Builder sidebar(SidebarState sidebarState) { this.sidebarState = sidebarState; return this; }
        public PageConfig build() { return new PageConfig(title, layout, sidebarState); }
    }
}
```

### 4.3 `Ui.pageConfig`

```java
/**
 * Configures page title and layout shell. Must be invoked before any other {@code Ui} method in
 * this {@code build()} run.
 *
 * @param config page configuration; never null
 */
void pageConfig(PageConfig config);
```

## 5. Wire protocol & root props

`UiBinder.buildRoot()` emits root props when `pageConfig` was called:

| Prop key | Type | Values |
|----------|------|--------|
| `pageTitle` | string | Browser tab title; empty omits change |
| `layout` | string | `"wide"` \| `"centered"` |
| `sidebarState` | string | `"expanded"` \| `"collapsed"` |

Constants live in `ComponentSpecs`: `PAGE_TITLE`, `LAYOUT`, `SIDEBAR_STATE`.

Backward compatible: absent props → client defaults (`wide`, `expanded`, document title unchanged).

## 6. Runtime — `UiBinder`

- Track `PageConfig pageConfig` and `boolean pageConfigLocked` (set true after first non-config call).
- `pageConfig(PageConfig)`: reject if locked; store config.
- `buildRoot()`: merge config into root props map (lowercase wire strings for enums).
- Any widget/layout method sets `pageConfigLocked = true` before work.

## 7. Client (`lumina-web`)

### 7.1 CSS file structure

```
lumina-web/
  lumina.css           ← @import aggregator (entry point unchanged)
  lumina-tokens.css    ← design tokens
  lumina-base.css      ← reset, body, fonts
  lumina-layout.css    ← app shell, sidebar, columns, expander, container
  lumina-components.css← all widget types
```

### 7.2 Design tokens (minimum set)

```css
:root {
  /* Color */
  --lumina-color-primary: #2563EB;
  --lumina-color-primary-hover: #1D4ED8;
  --lumina-color-bg: #FFFFFF;
  --lumina-color-bg-subtle: #F8FAFC;
  --lumina-color-sidebar: #F0F2F6;
  --lumina-color-border: #E2E8F0;
  --lumina-color-text: #0F172A;
  --lumina-color-text-muted: #64748B;

  /* Typography */
  --lumina-font-sans: "Inter", system-ui, -apple-system, sans-serif;
  --lumina-font-mono: ui-monospace, "Cascadia Code", monospace;
  --lumina-text-base: 1rem;
  --lumina-text-sm: 0.875rem;
  --lumina-text-lg: 1.125rem;

  /* Spacing (4px grid) */
  --lumina-space-1: 0.25rem;
  --lumina-space-2: 0.5rem;
  --lumina-space-3: 0.75rem;
  --lumina-space-4: 1rem;
  --lumina-space-6: 1.5rem;
  --lumina-space-8: 2rem;

  /* Radius & shadow */
  --lumina-radius-sm: 0.375rem;
  --lumina-radius-md: 0.5rem;
  --lumina-radius-lg: 0.75rem;
  --lumina-shadow-sm: 0 1px 2px rgb(15 23 42 / 0.06);
  --lumina-shadow-md: 0 4px 12px rgb(15 23 42 / 0.08);

  /* Layout */
  --lumina-sidebar-width: 21rem;
  --lumina-content-max-centered: 45rem;
  --lumina-content-padding: var(--lumina-space-8);
}

@media (prefers-color-scheme: dark) {
  :root { /* dark token overrides */ }
}
```

### 7.3 App shell behavior

`LuminaApp.render()` reads `this.tree.props` and:

1. Sets `document.title` when `pageTitle` non-empty.
2. Applies classes on `<lumina-app>`:
   - `lumina-layout-wide` | `lumina-layout-centered`
   - `lumina-sidebar-expanded` | `lumina-sidebar-collapsed`
   - `lumina-has-sidebar` when a sidebar child exists

**Wide mode:** full viewport width minus padding; sidebar fixed width; main flex-grow.

**Centered mode:** main content `max-width: var(--lumina-content-max-centered)` centered.

**Collapsed sidebar:** `width: 0`, `overflow: hidden`, border removed (visual only; no hamburger in P1.5).

### 7.4 Component polish checklist

Each widget type must have: consistent vertical rhythm (`margin-block`), focus-visible ring,
hover state (where interactive), token-based colors, dark mode pass.

| Type | Key upgrades |
|------|--------------|
| `button` | Primary fill, hover/active/disabled, min-height 2.5rem |
| `text_input`, `chat_input`, `file_upload` | Label typography, input border/focus |
| `user_message`, `ai_message` | Chat bubbles, alignment, streaming cursor |
| `markdown` | Heading scale, paragraph spacing |
| `code`, `json` | Monospace block, language header on code |
| `table` | Header row, borders, zebra optional |
| `progress` | Styled track |
| `expander` | Card panel, not raw UA `<details>` chrome |
| `sidebar` | Rail background, section spacing, nav button styling |
| `columns` | Gutters, align-items stretch |
| `container` | Optional bordered card variant via default padding |

## 8. Showcase app

**New:** `io.lumina.examples.showcase.ShowcaseApp` + `ShowcaseMain`

Demonstrates:

```java
ui.pageConfig(PageConfig.builder()
    .title("Lumina Showcase")
    .layout(PageLayout.WIDE)
    .sidebar(SidebarState.EXPANDED)
    .build());

ui.sidebar(nav -> {
    nav.markdown("## Navigation");
    nav.button("Home");
    nav.button("Settings");
});

ui.title("Dashboard");
ui.columns(3, cols -> {
    cols[0].markdown("### Users\n**1,284**");
    cols[1].markdown("### Revenue\n**$48.2k**");
    cols[2].progress(0.72);
});

ui.expander("Advanced", body -> body.code("java", "ui.pageConfig(...);"));
ui.textInput("Filter");
ui.button("Apply");
```

Update `lumina-examples/README.md` — Showcase as hero demo; LayoutDemo retained for IT.

## 9. Testing

| Test | Module | Asserts |
|------|--------|---------|
| `UiBinderPageConfigTest` | `lumina-runtime` | First-call rule; root props emitted |
| `UiSignatureTest` | `lumina-core` | `FakeUi` stub updated |
| `LuminaServerIT` extension | `lumina-web` | Snapshot root contains `layout`, `pageTitle` |
| `ProtocolCodecPageConfigTest` | `lumina-web` | Root props serialize in snapshot JSON |
| Full reactor | all | `mvn clean verify` green |

Visual QA: manual browser check of `ShowcaseApp` in light and dark mode.

## 10. Roadmap & ADR

- Insert **P1.5 UX Foundation** in `docs/VISION.md` between P1 and P2.
- Update status matrix (nested layout ✅, design system ❌ → P1.5 target).
- Add **ADR-013:** Design system ownership (framework CSS, token contract, page config API).
- P6 narrowed: tabs, dialogs, notifications, a11y hardening, theme toggle (not baseline polish).

## 11. Acceptance checklist

- [ ] `PageConfig` API + `Ui.pageConfig()` + root props wire
- [ ] CSS split: tokens, base, layout, components; dark mode tokens
- [ ] App shell: wide/centered, sidebar rail, collapsed visual state
- [ ] All 22 component types styled
- [ ] `ShowcaseApp` + README hero instructions
- [ ] ADR-013 + VISION.md updated
- [ ] Version `0.5.0-SNAPSHOT`
- [ ] `mvn clean verify` green

## 12. Out of scope (explicit)

- Tabs, dialogs, toasts, notifications
- Sidebar hamburger / animated collapse
- Author-defined themes or CSS escape hatches
- Playwright visual regression (optional follow-up)
- Routing, hot reload, SSE (remain P1 gaps, parallel track)
