# Lumina P9 — Developer Experience

**Date:** 2026-08-01  
**Version target:** `0.12.0-SNAPSHOT`

## Deliverables

1. **Hot reload:** `FileWatchReloader` in lumina-devtools watching classpath dirs; trigger session rebuild signal (broadcast intent or soft reconnect)
2. **CLI:** improve `lumina-cli` new/run if present; else document Maven exec
3. **CI:** `.github/workflows/ci.yml` — JDK 25, `mvn verify`
4. **Docs site:** expand `docs/` as static site source (PRODUCT, GUIDE, EXTENSIONS, ADRs index)

## Hot reload MVP

On file change under `src/main/java` of running app module, log + call registered `ReloadListener` that clears widget state optionally and forces snapshot — best-effort for examples.
