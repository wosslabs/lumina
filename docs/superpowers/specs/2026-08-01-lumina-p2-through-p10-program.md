# Lumina Product Program — P2 through P10

**Date:** 2026-08-01  
**Status:** Executing (no approval gates)  
**Branch:** `feature/lumina-p2-through-p10`  
**UX bar:** `docs/superpowers/specs/2026-08-01-lumina-ux-hard-reset-design.md` + constitution checklist  
**Start version:** `0.7.0-SNAPSHOT` → end target `1.0.0-SNAPSHOT` then `1.0.0` tagging docs

## Mission

Ship a coherent, world-class **open-source Java Streamlit-class framework** through roadmap P2–P10: remaining widgets, AI/agent surfaces, integrations, advanced UX, enterprise hooks, extensibility, DX, and 1.0 release packaging — with specs, plans, tests, and product docs at each phase.

## Execution rules

1. Each phase: **spec → plan → implement → `mvn clean verify` → docs/VISION update → commit(s)**.
2. Meet UX constitution for all new UI.
3. Prefer SPIs + working demos over incomplete cloud vendor SDKs (providers optional behind env config).
4. Self-correct on test failures before advancing.
5. No human approval pauses.

## Phase MVP definitions (done-when)

| Phase | MVP shipped when |
|-------|------------------|
| **P2** | checkbox, numberInput, selectbox, radio, slider, spinner, downloadButton + client/CSS + Showcase page + tests |
| **P3** | citation / RAG source / tool-call / usage (tokens, cost, latency) widgets + Showcase AI extras |
| **P4** | Spring AI ChatClient bridge API, provider SPI, Ollama/OpenAI-shaped config props, echo + optional live provider |
| **P5** | agent timeline, tool invocation row, approval step, memory panel widgets + demo app |
| **P6** | tabs, dialog, notification/toast, theme toggle (light/dark/system), responsive sidebar collapse |
| **P7** | AuthPrincipal hook, role gate helper, Micrometer meters, audit log SPI, i18n message source hook |
| **P8** | ComponentPlugin SPI, ThemeSpi, documented extension guide |
| **P9** | File-watch hot reload (devtools), CLI `new`/`run` improvements, GitHub Actions CI, docs site pages |
| **P10** | API freeze notes, CONTRIBUTING, MIGRATION, CHANGELOG, benchmarks stub, version `1.0.0-SNAPSHOT`, future roadmap |

## Version ladder

| After phase | Version |
|-------------|---------|
| P2 | 0.8.0-SNAPSHOT |
| P3 | 0.9.0-SNAPSHOT |
| P4–P5 | 0.10.0-SNAPSHOT |
| P6–P7 | 0.11.0-SNAPSHOT |
| P8–P9 | 0.12.0-SNAPSHOT |
| P10 | 1.0.0-SNAPSHOT |

## Product documents (created/updated along the way)

- `docs/PRODUCT.md` — what Lumina is / quick start / feature map  
- `docs/GUIDE.md` — author guide  
- `docs/EXTENSIONS.md` — plugin/theme/transport extension  
- `docs/MIGRATION.md` — pre-1.0 → 1.0  
- `CONTRIBUTING.md`, `CHANGELOG.md`  
- Per-phase specs under `docs/superpowers/specs/`  
- `docs/VISION.md` status matrix kept current  

## Risk posture

Full production parity with every cloud AI vendor, SSO IdP, and cluster store is **out of scope** for this program pass. Each enterprise/integration item ships as **usable SPI + reference implementation + docs** so the product is complete as a framework, not as a hosted SaaS.
