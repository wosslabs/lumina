# ADR-008: State model & server-side routing

## Status
Accepted

## Context
Session state today is a session-scoped map (`StateStore`) confined to the serial execution queue
(ADR-002, ADR-004). Multi-page apps and bookmarkable URLs are not shipped. Apps need typed
session state, server-owned navigation, and URL/query as addressable state that participates in
the same rerun loop as widget interactions.

## Decision
- Formalize typed, session-scoped state as the evolution of `StateStore`: values live for the
  session lifetime, are confined to the session execution queue, and remain the source of truth
  for app-owned and widget-persisted data.
- Routing is server-side: a `path → view` mapping selects which view/build runs for a session.
  There is no client-side router; navigation is an intent that updates addressable state and
  triggers a rerun (ADR-002).
- URL path and query are addressable state — navigable, bookmarkable, and restorable on reconnect
  alongside session state.
- State lifecycle (create, update, destroy) is owned by the session; idle TTL and eviction hooks
  are coordinated with ADR-010.
- This is architectural direction for later phases; it does not mandate a Phase 0 implementation.

## Consequences
Multi-page apps become possible without a browser SPA router. State lifecycle stays session-owned
and serial-per-session, so concurrent builds cannot race state or consume intents twice. Routing
and URL state add surface area for reconnect/snapshot semantics (ADR-012) but keep the thin-client
invariant: the server owns the tree and navigation outcome.
