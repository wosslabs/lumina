# Lumina P7 — Enterprise Hooks

**Date:** 2026-08-01  
**Version target:** `0.11.0-SNAPSHOT`

## Deliverables

1. `AuthContext` / `Principal` holder on session (optional filter hook in lumina-web)
2. `ui.rolesAllowed(Set<String> roles, Consumer<Ui> body)` — skip body if not allowed
3. Micrometer: session count, intent count, rerun timer (optional dependency)
4. `AuditLogger` SPI — log intent name + session id (no PII payloads by default)
5. `MessageSource` style `ui.t(String key)` optional — simple Map bundle in session/config

## Demo / docs

Document wiring in GUIDE + PRODUCT enterprise section.
