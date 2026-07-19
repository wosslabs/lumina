# ADR-006: Streaming token protocol and runtime frame sink

## Status
Accepted

## Context
Phase 2 streams LLM output token-by-token. The client is a tree-diff renderer;
mid-build partial updates must reuse the existing patch path so the client only
learns to append text, not to create elements from a side channel.

## Decision
- `ui.ai(TokenStream)` runs inside build() on the session virtual thread.
- A runtime RunSink delivers interim structural patches and text frames for the current run.
- At the ui.ai(TokenStream) call the runtime FLUSHES an interim patch (diff of
  children-so-far vs the last delivered baseline), creating the user + empty
  ai_message nodes through the normal patch path and advancing the baseline.
- Then it emits text-only frames: {type:"stream", id, op:"start"},
  {type:"stream", id, op:"append", text}, {type:"stream", id, op:"end"}.
- The ai_message node ends the run with full accumulated content. The final diff
  suppresses UPDATE_PROPS for streamed node ids (client already has the text).
- Frame ordering for a streamed node: flush(patch) -> start -> append* -> end.
- On reconnect a fresh snapshot carries full content; stream frames are not replayed.

## Consequences
Serial-per-session semantics unchanged; client change is minimal (append text by
node id). Redundant final update avoided via suppression. Cancellation deferred.
