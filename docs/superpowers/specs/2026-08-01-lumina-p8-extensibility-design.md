# Lumina P8 — Extensibility

**Date:** 2026-08-01  
**Version target:** `0.12.0-SNAPSHOT`

## SPIs

1. `ComponentRenderer` client registration remains framework-owned; server `ComponentType` contribution via ServiceLoader `ComponentContribution` (type name + prop schema doc)
2. `ThemeSpi` — supply extra CSS resource path
3. `TransportSpi` — document WebSocket; SSE adapter stub implementing same intent runner
4. `AiProvider` from P4

## Docs

`docs/EXTENSIONS.md` with examples.
