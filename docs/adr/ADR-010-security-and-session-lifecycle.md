# ADR-010: Security & session lifecycle

## Status
Accepted

## Context
`lumina-web` already mitigates Cross-Site WebSocket Hijacking via origin checks at upgrade
(ADR-005). Enterprise auth/SSO, RBAC, strong session identifiers, upload bounds, and idle
eviction are required for Phase 7 but must not be bolted on after the session and transport
models harden. Hook points and lifecycle rules need to be decided now; full enforcement ships
later.

## Decision
- Keep existing origin / CSWSH checks at the WebSocket (and future transport) handshake.
- Design auth/SSO and RBAC as hook points that integrate through the Spring Security filter
  chain when apps use Spring Boot — not as a bespoke auth framework inside `lumina-core`.
- Session identifiers must use cryptographically strong entropy; session create/destroy remains
  tied to transport lifecycle with explicit TTL and eviction for idle or over-cap sessions.
- Bound file upload size and count per session to limit abuse and memory growth.
- Enforcement and hardening land in Phase 7; Phase 0 only records the model and hook points.
- This is architectural direction for later phases; it does not mandate a Phase 0 implementation.

## Consequences
Security controls can be enforced in Phase 7 without redesigning session or transport ownership.
Apps that embed without Spring keep a minimal surface (origin, session caps, TTL); Spring apps
gain filter-chain integration for SSO/RBAC. Session TTL/eviction pairs with ADR-008 state
lifecycle to prevent unbounded per-session memory growth.
