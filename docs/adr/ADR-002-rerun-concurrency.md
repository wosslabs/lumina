# ADR-002: Rerun concurrency

## Status
Accepted

## Context
Lumina reruns application code after each session interaction. A rerun reads and mutates
session-scoped app state and widget intents while producing a replacement component tree. Concurrent
builds for one session could consume the same intent twice, lose state updates, or publish trees out
of order.

## Decision
Each session owns one serial execution queue. Initial builds and interaction-triggered reruns enter
that queue and execute in submission order. No two application builds for the same session may run
concurrently.

Queue work runs on virtual threads so blocked sessions do not consume platform threads. Sessions
have independent queues and may execute concurrently with one another. The queue is implemented by
the runtime session runner in Task 7; state and binder classes rely on that confinement rather than
adding internal synchronization.

## Consequences
Widget intents are consumed at most once and each rerun observes all state changes from earlier
reruns in the session. Slow application code delays later work for its own session but does not
serialize unrelated sessions. Runtime code must route every build and session-state access through
the session queue.
