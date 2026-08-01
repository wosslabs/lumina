# Lumina — P1 Server-side routing (MVP)

**Status:** Approved (2026-07-26)
**Depends on:** ADR-008, P1.5 UX Foundation (`0.5.0-SNAPSHOT`)
**Version target:** `0.6.0-SNAPSHOT`

## 1. Goal

Ship **server-side routing** so multi-page Streamlit-style apps can switch views in pure Java,
with bookmarkable URLs and reconnect path restore — no client-side SPA router.

## 2. Decisions

| Topic | Choice |
|-------|--------|
| Route storage | Session `StateStore` key `__lumina.path` (framework reserved) |
| Default path | `/` |
| Author API | `ui.path()` read; `ui.navigate(String path)` write (same-run visible) |
| Connect | `connect` intent payload `{ "path": "..." }` from client `location.pathname` |
| Navigate intent | `{ "type":"intent", "name":"navigate", "payload":{ "path":"/settings" } }` |
| Client URL | `history.pushState` on navigate; read path on connect |
| Query string | Deferred (path only in MVP) |
| Router registry | Deferred — apps use `switch (ui.path())` manually |
| `LuminaApp` API | Unchanged — routing is opt-in via `Ui` |

## 3. Author pattern

```java
@Override
public void build(Ui ui) {
    ui.pageConfig(...);
    ui.sidebar(nav -> {
        if (nav.button("Home")) ui.navigate("/");
        if (nav.button("Settings")) ui.navigate("/settings");
    });
    switch (ui.path()) {
        case "/settings" -> buildSettings(ui);
        default -> buildHome(ui);
    }
}
```

## 4. Wire protocol

**Connect** (additive payload keys):
```json
{ "type": "intent", "name": "connect", "payload": { "path": "/settings" } }
```

**Navigate** (new intent):
```json
{ "type": "intent", "name": "navigate", "payload": { "path": "/about" } }
```

**Snapshot root props** (additive): `"path": "/settings"` when non-default or always emit.

## 5. Testing

- `UiBinderRoutingTest` — path default, navigate, same-run visibility
- `AppRunnerRoutingTest` — connect + navigate intents
- `LuminaServerIT` — multi-path snapshot differs after navigate
- Extend `ShowcaseApp` with Home + About pages

## 6. Out of scope

- Query params, route params, typed router registry
- SSE transport, hot reload
