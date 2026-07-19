# ADR-009: Extensibility SPIs

## Status
Accepted

## Context
Lumina must grow custom components, AI providers, transports, rendering hooks, and themes without
rewriting the kernel. A full public plugin SDK and packaging model belong in Phase 8; delaying
all seam design until then would force core rewrites. Seams need to be named and kept
internally stable now so later phases and the SDK can plug in cleanly.

## Decision
- Name and treat as internal-stable SPI seams (not a public plugin SDK yet):
  - **Component registry** — register custom component types/factories.
  - **AI provider** — adapt external models to the core AI seam (ADR-011).
  - **Transport** — WebSocket today; SSE and alternate channels later (ADR-012).
  - **Rendering** — server tree → client presentation hooks.
  - **Theme** — theming / dark-mode hook points (Phase 6+).
- Keep these seams internal and stable enough for first-party modules and future adapters;
  defer the public plugin SDK, packaging, and extension guides to Phase 8.
- Core and runtime depend on SPI abstractions, not on concrete provider or transport types
  (consistent with ADR-001 module boundaries).
- This is architectural direction for later phases; it does not mandate a Phase 0 implementation.

## Consequences
The core becomes plugin-ready without shipping an SDK. Phase 7–8 work can add clustering,
themes, and third-party extensions at the named seams. Premature public API surface is avoided;
internal SPI shapes may still evolve until the Phase 8 freeze.
