# Lumina — UX Hard Reset (W0→W2)

**Date:** 2026-08-01  
**Status:** Approved for planning  
**Depends on:** P1 kernel + routing (`0.6.0`), P1.5 tokens/shell (provisional)  
**Version target:** `0.7.0-SNAPSHOT`

## 1. Goal

Treat the current thin-client UI as provisional. Rebuild app chrome, design system, and all shipped widgets to **enterprise product-shell** quality, then use that bar as the gate for P2→P10.

This is a **full-stack UX rethink**: client + CSS/tokens + author APIs for shell structure — not a CSS-only restyle. Runtime kernel (rerun loop, diff, WebSocket, routing) stays.

## 2. Decisions

| Topic | Choice |
|-------|--------|
| Strategy | Hard reset of UI layer (not forward-only polish) |
| Scope wave | W0 constitution + W1 chrome/APIs + W2 widget rebuild in one epic |
| Execution | Spec-first, one branch, gated checkpoints (Approach A) |
| Visual direction | Enterprise product shell — denser, neutral chrome; restrained accent |
| Navigation | Structured sidebar slots: `brand` / `nav` / `footer` |
| Main chrome | Compact top banner + in-page `ui.title()` as view H1 |
| P2 widgets | Deferred until after this epic |

## 3. Waves

| Wave | Focus | Outcome |
|------|--------|---------|
| **W0** | UX constitution | Written standards + PR checklist |
| **W1** | App chrome / layout model | Landmarks, banner, structured sidebar, optional header |
| **W2** | Rebuild shipped widgets | Every existing control meets W0 |

After merge: P1 gaps (hot reload, SSE) and P2 widgets proceed under W0.

## 4. UX constitution (W0)

Non-negotiable for this epic and later phases:

1. **Semantics first** — landmarks (`banner`, `navigation`, `main`, `contentinfo` where applicable); one H1 per view; labeled controls.
2. **Keyboard & focus** — full keyboard use; visible `:focus-visible`; no focus traps except intentional modals (none this epic).
3. **Predictable density** — 4/8px spacing; denser enterprise chrome; ≥40px targets for primary controls.
4. **Quiet chrome** — neutral surfaces; accent for primary actions and current nav only (retire Streamlit-red as default brand).
5. **Motion** — short, purposeful; honor `prefers-reduced-motion`.
6. **Forms** — visible labels; optional `help` as accessible description; full validation later.
7. **Contrast** — WCAG AA for text/icons on light and dark tokens.
8. **Thin client** — zero author HTML/CSS/JS; framework owns chrome and widgets.

Deliverable: constitution checklist committed under `docs/superpowers/specs/` (can live as §4 of this spec plus a short PR checklist file).

## 5. Shell & author APIs (W1)

### Client chrome (framework-owned)

- Top **banner**: product name from `PageConfig.title`, optional utilities later
- **Sidebar** regions: brand · nav · footer
- **Main**: page content; first `ui.title()` is the view H1

### Author API shape

```java
ui.pageConfig(PageConfig.builder()
    .title("Lumina")
    .layout(PageLayout.WIDE)
    .sidebar(SidebarState.EXPANDED)
    .build());

ui.sidebar(sb -> {
    sb.brand(b -> { b.markdown("## Lumina"); b.text("…"); });
    sb.nav(nav -> {
        nav.page("Home", "/");
        nav.page("About", "/about");
    });
    sb.footer(f -> { if (f.button("Reset demo")) { /* … */ } });
});

ui.header(h -> {
    h.title("Optional context line"); // not a second H1
});
```

### Behavior

- `nav.page(label, path)` renders a nav control; click issues navigate; `aria-current="page"` when `ui.path()` matches.
- Freeform widgets allowed in `brand` / `footer`.
- Nav is structured only (`nav.page`).
- Legacy `ui.sidebar(Consumer<Ui>)` remains: freeform body still works (default slot) so existing apps don’t hard-crash; `aria-current` only for `nav.page`.
- Client derives landmarks from tree shape.

### Out of scope (this epic)

Mobile drawer, command palette, theme toggle, collapse animation beyond existing sidebar state (P6+).

## 6. Widget rebuild (W2)

Rebuild all shipped controls to constitution + new tokens:

| Area | Types |
|------|--------|
| Inputs | button, text_input, file_upload, chat_input |
| Display | title, text, markdown, code, json, table, image, progress |
| Chat | user_message, ai_message |
| Layout | container, columns, expander (accessible disclosure; no double-toggle) |
| Shell nodes | sidebar_brand, sidebar_nav, nav_page, sidebar_footer, app_header |

### API polish

- Optional `help` on inputs where natural.
- `disabled` on button / text_input / file_upload if low-cost; otherwise defer.

### Not in this epic

number, checkbox, select, radio, slider, spinner, download — **P2**, after merge, on the new standard.

## 7. Wire / tree

- New component types for shell regions and `nav_page` (props: `label`, `path`). Client sets `aria-current` by comparing each item’s path to root `path`.
- Root continues to carry `path`, pageConfig props.
- Routing intents unchanged (`connect` with path, `navigate`, widget `click`/`input`).

## 8. Migration & version

- Target version: **`0.7.0-SNAPSHOT`** (pre-1.0 breaking for shell APIs OK).
- Showcase + examples migrate to brand/nav/footer + header.
- Design tokens replaced for enterprise neutral shell.
- `ui.path` / `ui.navigate` unchanged.

## 9. Testing & acceptance

- Unit: shell slots in `UiBinder`; nav page wiring; header binding.
- IT: nav click updates path; expander toggle; snapshots include shell structure.
- Manual/Showcase: keyboard tour, focus-visible, reduced-motion, light/dark.
- Gate: `mvn clean verify`.
- Acceptance: Showcase demonstrates enterprise shell; constitution checklist satisfied for shipped widgets.

## 10. Docs / roadmap

- This spec + UX constitution checklist.
- Update `docs/VISION.md`: UX hard-reset wave; resume P2 widgets after `0.7.0`.

## 11. Non-goals

- Rewriting session/runtime kernel
- SSE / hot reload (follow-on P1 gaps)
- Full P2 widget set
- Pixel-perfect custom design systems authored by app developers
