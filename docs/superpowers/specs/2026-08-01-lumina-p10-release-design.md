# Lumina P10 — 1.0 Release Packaging

**Date:** 2026-08-01  
**Version target:** `1.0.0-SNAPSHOT`

## Deliverables

1. `CHANGELOG.md` summarizing P0–P10
2. `CONTRIBUTING.md` — build, test, PR checklist (incl. UX constitution)
3. `docs/MIGRATION.md` — 0.x → 1.0 notes (shell APIs, connect-with-path, etc.)
4. API freeze statement in VISION / PRODUCT
5. Minimal JMH or simple throughput note in `docs/BENCHMARKS.md` (optional microbench stub)
6. Future roadmap section post-1.0
7. Version `1.0.0-SNAPSHOT` on all modules

## Definition of done

`mvn clean verify` green; docs coherent; Showcase covers widgets/AI/agent/theme; CI workflow present.
