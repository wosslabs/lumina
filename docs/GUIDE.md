# Lumina Author Guide

Framework-owned UI in pure Java. See also [PRODUCT.md](PRODUCT.md) and [VISION.md](VISION.md).

## App entry

```java
public class MyApp implements LuminaApp {
  @Override public void build(Ui ui) {
    ui.pageConfig(PageConfig.builder().title("My App").build());
    ui.sidebar(sb -> {
      sb.brand(b -> b.markdown("## My App"));
      sb.nav(nav -> nav.page("Home", "/"));
    });
    ui.title("Hello");
  }
}
```

## Routing

- `ui.path()` — current path  
- `ui.navigate("/x")` — set path this run  
- Client sends `connect` with `location.pathname`

## Shell

Prefer `sidebar.brand` / `sidebar.nav` / `sidebar.footer` and optional `ui.header`.

## Widgets

*(P2 fills this section.)*

## AI

*(P3–P5 fill this section.)*

## UX standards

PRs touching UI must satisfy `docs/superpowers/specs/2026-08-01-lumina-ux-constitution-checklist.md`.
