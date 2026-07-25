# ADR-013: Design system ownership & page configuration

## Status
Accepted. Implemented in `0.5.0-SNAPSHOT` (P1.5 UX Foundation).

## Context
Lumina's thin client ships framework-owned CSS and Web Components. After nested layout
(`0.4.0-SNAPSHOT`), structure works but the default appearance is minimal — insufficient for
Streamlit-class first impressions. Visual polish was previously deferred to Phase 6, which
risks shipping unstyled P2 widgets and forcing a costly restyle pass.

Authors must not write HTML/CSS/JS (hard invariant). The framework therefore owns the design
system, token contract, app shell, and page-level configuration API.

## Decision
- **Framework-owned design system:** CSS lives in `lumina-web` (`lumina-tokens.css`,
  `lumina-base.css`, `lumina-layout.css`, `lumina-components.css`, aggregated by `lumina.css`).
  Application code cannot inject styles.
- **Token contract:** Components and layout use `--lumina-*` custom properties. New widgets
  must consume tokens; ad-hoc colors in component CSS are forbidden.
- **Page configuration API:** `Ui.pageConfig(PageConfig)` is the Streamlit
  `set_page_config` equivalent. It must be the first `Ui` call per `build()`. Configuration
  is emitted on the root node's `props` (`pageTitle`, `layout`, `sidebarState`) — no new wire
  message types.
- **Layout modes:** `WIDE` (default, full-width main) and `CENTERED` (max-width column).
  Sidebar visual state (`EXPANDED` / `COLLAPSED`) is a prop; interactive collapse UI is P6.
- **Dark mode baseline:** `prefers-color-scheme: dark` token overrides in P1.5; manual theme
  toggle and author themes deferred (P6 / P8 Theme SPI).
- **P2 gate:** New core widgets do not ship until they pass the P1.5 component polish checklist.

## Consequences
- Professional default UX without violating the zero-author-CSS invariant.
- CSS maintenance grows in `lumina-web`; split files keep responsibilities clear.
- Root props become the page-metadata channel; future metadata (favicon, meta tags) can extend
  the same pattern.
- Theme SPI (ADR-009) remains named but unimplemented publicly until P8; internal tokens are
  the stable seam for later author themes.
