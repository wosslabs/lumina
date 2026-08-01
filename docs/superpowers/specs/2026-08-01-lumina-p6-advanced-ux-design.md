# Lumina P6 — Advanced Layout & UX

**Date:** 2026-08-01  
**Version target:** `0.11.0-SNAPSHOT`

## APIs

```java
void tabs(List<String> labels, Consumer<Ui[]> panels); // equal to columns but tab chrome
void dialog(String title, Consumer<Ui> body); // open via widget state / button pattern
void notify(String message); // toast — session flash for one rerun or client-only ephemeral via root prop queue
void themeToggle(); // writes preference to StateStore __lumina.theme; client applies data-theme
```

## Client

- Tabs: tablist/tab/tabpanel a11y
- Dialog: modal with focus trap minimal (Esc closes → intent)
- Toast: aria-live polite region
- Theme: light/dark/system via tokens already present

## Responsive

Collapsed sidebar CSS under 768px (overlay or icon rail).
