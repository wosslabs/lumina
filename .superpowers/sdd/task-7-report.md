# Task 7 Report: AppRunner + SessionManager (headless rerun loop)

## Status
Complete.

## Changes
- Added `Intent` record (`connect`/`click`/`input`/`submit_chat` factories) as the client-action
  input to a session rerun.
- Added `AppRunner`: package-private, one instance per session, stateful (retains the previous
  tree). Applies the intent to `WidgetState` by `targetId` (used directly as the widget key),
  rebuilds via `UiBinder`, and diffs against the previous tree with `TreeDiffer`.
- Added `SessionExecutor`: a per-session `LinkedBlockingQueue<Runnable>` drained by one dedicated
  virtual thread, so submissions for one session always run one at a time, in order, without
  blocking a platform thread.
- Added `SessionHandle` (`submit(Intent): CompletableFuture<RunResult>`, `close()`) and
  `SessionManager` (`create(): SessionHandle`), each session fully isolated (own `SessionState`,
  `AppRunner`, `SessionExecutor`).
- Added `io.lumina.Lumina.sessionManager(LuminaApp)` convenience factory.
- Extended `RunResult` from `record RunResult(ComponentNode root)` to
  `record RunResult(ComponentNode root, List<PatchOp> patches, boolean fullSnapshot, String error)`,
  keeping a secondary `RunResult(ComponentNode root)` constructor (patches empty, no snapshot/error)
  so the existing `UiBinderTest.runResultExposesRoot` test compiles and passes unchanged. Added
  `snapshot(root)`, `patched(root, patches)`, `error(previousRoot, message)` factories and
  `hasError()`.

## Resolutions applied
- **Connect → full snapshot**: `AppRunner` returns `RunResult.snapshot(root)` whenever there is no
  prior tree yet (first successful run for the session), with empty `patches()` and
  `fullSnapshot() == true`, regardless of which intent triggered it.
- **Intents apply by `targetId` as widget key**: `click`→`widgets.set(key, true)`,
  `input`→`widgets.set(key, value)`, `submit_chat`→`widgets.setChatSubmit(key, value)`, matching
  the keys `UiBinder` assigns to interactive nodes.
- **User exceptions**: caught in `AppRunner.run`; the previous tree is kept, no patches are
  computed, and `RunResult.error(previousRoot, message)` surfaces the failure message. (Only edge
  case not covered by this rule: a user exception on the *very first* run, where there is no
  previous tree to keep — that case rethrows as `LuminaException`, since a `RunResult` cannot be
  constructed without a root. See Concerns.)
- **Runtime stays Spring/Jetty-free**: no new dependencies were added; `lumina-runtime/pom.xml` is
  unchanged.

## TDD evidence
### RED
`AppRunnerTest` (6 cases, including the brief's exact "Hello AI" connect→chat-submit flow) was
written before any of `Intent`/`SessionHandle`/`SessionExecutor`/`SessionManager`/`AppRunner`
existed.

```text
mvn -q -pl lumina-runtime -am test -Dtest=AppRunnerTest -Dsurefire.failIfNoSpecifiedTests=false
```

Result: exit 1, compilation failure — `cannot find symbol` for `SessionManager`, `SessionHandle`,
`Intent`, and `RunResult.fullSnapshot()/patches()/error()`.

### GREEN
Implemented `Intent`, `AppRunner`, `SessionExecutor`, `SessionHandle`, `SessionManager`,
`io.lumina.Lumina`, and extended `RunResult`.

```text
mvn -q -pl lumina-runtime -am test -Dtest=AppRunnerTest -Dsurefire.failIfNoSpecifiedTests=false
```

Result: exit 0. `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`.

Test cases:
1. `connectThenChatSubmitProducesAiEchoInTree` — brief's Hello AI flow: connect, find the chat
   input's id, submit a chat intent, assert `USER_MESSAGE`/`AI_MESSAGE` appear in the rerun tree.
2. `connectProducesFullSnapshotWithEmptyPatches` — connect sets `fullSnapshot=true`, `patches`
   empty, `error` null.
3. `subsequentIntentProducesIncrementalPatches` — a click after connect yields `fullSnapshot=false`
   and non-empty patches reflecting the newly added node.
4. `userExceptionKeepsPreviousTreeAndSurfacesError` — a click that throws keeps the prior root
   (`equals` on the record) and surfaces the exception message via `error()`, with no patches.
5. `intentsForSameSessionExecuteSerially` — 20 concurrently-submitted click intents against
   unsynchronized mutable session state (`int[]`) are proven serial: the last future observed
   resolves to a tree with the fully-incremented counter, which would flake under interleaving.
6. `sessionsAreIsolatedPerHandle` — two sessions on the same `LuminaApp`/`SessionManager` do not
   share widget state.

## Verification
```text
mvn -q clean compile
mvn -q clean test
mvn -q clean package
git diff --check
```

All four commands exited 0. Full reactor test run: `lumina-core`, `lumina-session` (9 tests),
`lumina-runtime` (`TreeDifferTest` 8, `UiBinderTest` 9, `AppRunnerTest` 6) all pass; no test files
outside `lumina-runtime` were touched. IDE diagnostics report no linter errors on any changed file.

## Concerns
- **First-run user exception has no `RunResult` to return.** `RunResult.root()` is
  `@Nonnull`-by-contract (the canonical constructor calls `Objects.requireNonNull`), so a failure
  on the very first `build(ui)` call for a session — before any tree exists — cannot be represented
  as an error `RunResult`. `AppRunner` rethrows as `LuminaException` in that case, which surfaces as
  an exceptionally-completed `CompletableFuture` from `SessionHandle.submit`. This is untested by
  the brief's flow (which always connects successfully first) but callers (e.g. the future web
  socket endpoint) must handle a failed connect future, not just an `error()`-carrying `RunResult`.
- **`AppRunner` is package-private.** The brief's "Produces" list included
  `AppRunner.run(LuminaApp, SessionState, Intent): RunResult` as a signature, but nothing in Task 7
  requires cross-package access — `AppRunnerTest` lives in the same `io.lumina.runtime` package.
  Kept it package-private (accessed only through `SessionHandle`) to minimize public surface;
  widen to `public` later if a future task needs to call it directly.
- **No session registry/lookup in `SessionManager`.** The design doc's component diagram describes
  `SessionManager` as "creates/looks up sessions," but Task 7's tested interface is only
  `create(): SessionHandle` — the caller (e.g. a future WebSocket endpoint) is expected to hold the
  returned handle itself. Left `SessionManager` as a pure factory per the minimal-change principle;
  a lookup-by-id map can be added when a consumer actually needs it.
- **`SessionExecutor` virtual threads are not reaped automatically.** Each `SessionHandle` starts
  one virtual thread that loops until `close()` interrupts it. Nothing in Task 7 calls `close()`
  (no session teardown trigger exists yet), so long-running processes that create many sessions
  without an explicit close will leak parked virtual threads. This is cheap per-thread but should
  be wired to the future WebSocket disconnect handler in the web module.

## Commit
`feat(runtime): add session-scoped AppRunner and serial executor`

## Critical/Important review follow-up

### Changes
- Initial user-code build failures now return an error `RunResult` instead of completing the
  session future exceptionally. `RunResult.root()` may be `null` only when an error is present and
  no successful tree exists yet.
- `SessionExecutor.shutdown()` now atomically marks the executor closed, exceptionally completes
  queued submissions, and rejects later submissions with
  `IllegalStateException("Session executor is shut down")`.
- Added an initial-connect failure case to `AppRunnerTest` and two focused
  `SessionExecutorTest` shutdown cases.

### TDD evidence
- RED: `AppRunnerTest#failedInitialConnectReturnsErrorResult` errored with
  `CompletionException: LuminaException: Failed to build initial UI for intent 'connect'`.
- RED: the combined regression run hung because the queued future was never completed; the run was
  stopped after 120 seconds.
- GREEN:
  `mvn -q -pl lumina-runtime -am test -Dtest=AppRunnerTest,SessionExecutorTest -Dsurefire.failIfNoSpecifiedTests=false`
  exited 0. `AppRunnerTest`: 7 tests; `SessionExecutorTest`: 2 tests; 0 failures/errors/skips.
