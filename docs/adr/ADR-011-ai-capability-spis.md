# ADR-011: AI capability SPIs

## Status
Accepted

## Context
Phase 2/4 AI integration must support multiple providers (Spring AI adapters and others) without
leaking provider types into `lumina-core`. The neutral `ChatClient` / `TokenStream` seam already
exists as the app-facing entry point. Not every provider supports tools, embeddings/RAG, or
usage/cost reporting; forcing a single fat interface would either leak types or invent no-ops.

## Decision
- Keep the framework-neutral `ChatClient` / `TokenStream` seam in `lumina-core` as the single
  primary app-facing AI entry point; providers ship as thin adapters (e.g. `lumina-spring-ai`).
- Add optional capability interfaces beside that seam for tool-calling, embeddings/RAG, and
  usage/cost (and related metadata such as latency where useful).
- Providers implement only the capabilities they support; the runtime and UI degrade gracefully
  when a capability is absent (hide or no-op the related UI, do not fail the core chat path).
- Core and runtime never depend on concrete provider SDKs — only on the neutral types and
  optional capability SPIs (ADR-001, ADR-009).
- This is architectural direction for later phases; it does not mandate a Phase 0 implementation.

## Consequences
Multi-provider work in Phase 4 can proceed without provider types escaping into core. Optional
capabilities unlock Phase 3/5 surfaces (tool viz, RAG sources, token/cost) when available.
Graceful degradation keeps simpler providers first-class rather than second-class stubs.
