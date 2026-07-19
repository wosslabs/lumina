# ADR-012: Transport evolution

## Status
Accepted

## Context
JSON over WebSocket is the primary transport today (`lumina-web`, ADR-005), with wire shapes and
diff ops in ADR-003 and streaming frames in ADR-006. Some environments block or interrupt
WebSockets; reconnect must restore interactivity without replaying stream frames. Phase 7
clustering also needs the transport boundary to be swappable without rewriting the runtime.

## Decision
- WebSocket remains the primary transport for interactive sessions (ADR-005, ADR-006).
- Define an SSE (or equivalent unidirectional-plus-intent) fallback path for environments where
  WebSocket is unavailable or undesirable, behind the same session/runtime contract.
- On reconnect or resume, deliver a fresh snapshot with full tree (and addressable URL/state as
  applicable); do not replay in-flight stream frames (consistent with ADR-006).
- Treat transport as an SPI (ADR-009) so Phase 7 clustering and alternate channels can plug in
  without core rewrites; single-node in-memory sessions remain the default.
- This is architectural direction for later phases; it does not mandate a Phase 0 implementation.

## Consequences
A resilience path (SSE fallback + snapshot reconnect) is defined without changing the single-node
default. Runtime and protocol stay transport-agnostic at the SPI boundary. Clustering work in
Phase 7 can replace or wrap the transport/session store without altering ADR-003 message shapes
or the thin-client apply model.
