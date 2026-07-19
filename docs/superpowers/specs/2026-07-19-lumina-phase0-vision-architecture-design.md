# Lumina Phase 0 — Vision & Architecture (+ Platform Upgrade)

**Status:** Approved (design dialogue 2026-07-19)
**Depends on:** Phase 1 (`2026-07-18-lumina-phase1-design.md`), Phase 2 (`2026-07-18-lumina-phase2-streaming-design.md`)
**Version target:** `0.3.0-SNAPSHOT` (platform upgrade is a significant, pre-1.0 change)

## 1. Goal

Establish the canonical, authoritative vision and architecture for Lumina across its full
lifecycle, adopting an 11-phase roadmap (Phase 0–10), and reconcile it with the code already
shipped. Phase 0 produces **documentation and one platform upgrade** — no feature code.

Two deliverable groups:

- **A. Platform upgrade** — Java 21→25, Spring Boot 3.4.4→4.1.0, Spring AI 1.1.2→2.0.0 (plus the
  transitive consequences these majors force). Done **first**, so the architecture docs describe the
  real, verified stack.
- **B. Vision & Architecture docs** — `docs/VISION.md`, `docs/ARCHITECTURE.md`, and ADR-007…012,
  reusing ADR-001…006 by reference.

## 2. Decisions locked in design dialogue

| Topic | Choice |
|-------|--------|
| Deliverable form | Authoritative `VISION.md` + `ARCHITECTURE.md` (C4 + 14 sections) + new ADRs for open decisions; reuse ADR-001…006 by reference |
| Roadmap numbering | Adopt the user's Phase 0–10 numbering as canonical; a **status matrix** marks already-built parts of P1–P4 as partial; resume from earliest gaps |
| Scale model | Single-node default (in-memory sessions); state store + transport defined as SPIs so Phase 7 clustering slots in without core rewrites |
| Client rendering | Thin, build-step-free client (vanilla Web Components + server-driven tree diff); zero user-authored HTML/CSS/JS is a hard invariant; grows only for layout/theming |
| Component model | Evolve today's **flat** child list into a **nested** `ComponentNode` tree with stable keys, so layout containers can hold children (ADR-007) |
| State & routing | Formalize typed, session-scoped state (evolving `StateStore`) + simple server-side multi-page routing (`path → view`), URL/query as addressable state (ADR-008) |
| Extensibility | Design clean SPI seams now (component registry, AI-provider, transport, rendering, theme); defer the public plugin SDK/packaging to Phase 8 (ADR-009) |
| AI seam | Framework-neutral `ChatClient`/`TokenStream` SPI in `lumina-core` as the single seam; providers ship as thin adapters; add optional capability SPIs (tools, embeddings/RAG, usage/cost) (ADR-011) |
| Performance | Concrete, testable targets proposed now; benchmarked in Phase 10 |
| Platform | Java 25, Spring Boot 4.1.0, Spring AI 2.0.0 (see §5) |

## 3. Canonical roadmap (Phase 0–10)

Adopted verbatim from the product owner, numbering is canonical going forward:

- **P0 Vision & Architecture** — this spec's deliverables.
- **P1 Framework Kernel (MVP)** — embedded server, component tree, rendering, diff engine, session
  state, routing, basic layout, hot reload, WebSocket/SSE transport, Hello World.
- **P2 Core UI Components** — text, markdown, code, buttons, text/number input, checkbox, select,
  radio, slider, images, tables, JSON viewer, progress, spinners, file upload/download.
- **P3 AI Components** — chat UI, streaming tokens, prompt editor, history, markdown, code
  highlighting, citations, RAG sources, tool-call visualization, token usage, cost, latency.
- **P4 Spring AI & LangChain4j Integration** — auto-config + first-class ChatClient/ChatModel;
  Bedrock, OpenAI, Azure, Vertex, Ollama; `ui.ai(chatClient.prompt(prompt))`.
- **P5 Agentic UI** — agent timeline, tool invocation viewer, reasoning graph, workflow viz, memory
  inspection, multi-agent, human approval steps, live execution logs.
- **P6 Advanced Layout & UX** — tabs, sidebars, expanders, dialogs, notifications, responsive
  layouts, theming, dark mode, accessibility, keyboard shortcuts.
- **P7 Enterprise** — auth/SSO hooks, RBAC, session clustering, observability
  (Micrometer/OpenTelemetry), audit logging, i18n, config profiles, security hardening.
- **P8 Extensibility** — plugin API, component SDK, theme SDK, transport SPI, rendering SPI, AI
  provider SPI, packaging & extension guides.
- **P9 Developer Experience** — Spring Boot starter, CLI (new/run/package/deploy), hot reload, live
  preview, starter templates, samples, Maven Central publishing, GitHub Actions, docs site.
- **P10 1.0 Release** — API freeze, semver, binary-compat checks, benchmarks, docs, migration guide,
  contribution guidelines, future roadmap.

## 4. Status matrix (reconciliation with shipped code)

Legend: ✅ done · ◐ partial · ❌ not started

| Phase | Item | Status | Notes |
|-------|------|--------|-------|
| P1 | Embedded server | ✅ | Jetty 12 embedded (`lumina-web`, ADR-005) |
| P1 | Component tree | ◐ | Immutable `ComponentNode`, but **flat children only** |
| P1 | Rendering engine | ◐ | Server-driven; no nested layout |
| P1 | Diff engine | ✅ | `TreeDiffer` + patch ops (ADR-003) |
| P1 | Session state | ◐ | `StateStore` session map; not typed; no routing |
| P1 | Routing | ❌ | — |
| P1 | Basic layout | ❌ | No columns/tabs/container |
| P1 | Hot reload | ❌ | `NoOpReloader` stub only |
| P1 | WebSocket transport | ✅ | `LuminaWebSocketEndpoint` + backpressure |
| P1 | SSE transport | ❌ | — |
| P1 | Hello World | ✅ | `lumina-examples` |
| P2 | text/markdown/code/button/text-input/table/json/image/progress/file-upload | ✅ | `lumina-components` |
| P2 | number/checkbox/select/radio/slider/spinner/download | ❌ | — |
| P3 | chat UI, streaming tokens, history, markdown, code highlight | ✅ | Phase 2 delivered |
| P3 | citations, RAG sources, tool-call viz, token usage, cost, latency | ❌ | — |
| P4 | Spring AI ChatClient auto-config | ◐ | `lumina-spring-ai`; echo-tested only, no real model wired |
| P4 | LangChain4j, Bedrock, OpenAI, Azure, Vertex, Ollama | ❌ | — |
| P5–P10 | all | ❌ | Not started |

**Resume point after Phase 0:** P1 gaps (nested layout, routing, hot reload, SSE) → then finish P2 widgets.

## 5. Platform upgrade (verified 2026-07-18)

### 5.1 Target versions

| Component | From | To | Source of truth |
|-----------|------|----|-----------------|
| Java (`java.version`, `maven.compiler.release`) | 21 | **25** | Spring Boot 4 first-class Java 25 support |
| Spring Boot (`spring-boot-dependencies` BOM) | 3.4.4 | **4.1.0** | Latest GA (2026-06-10); Spring AI 2.0 starters align to 4.1.0 |
| Spring AI (`spring-ai-bom`) | 1.1.2 | **2.0.0** | GA (2026-06-12); supports Boot 4.0/4.1, Spring Framework 7 |
| Jetty (`jetty-bom`, embedded in `lumina-web`) | 12.0.16 | **12.1.x** (latest patch, resolved at build time) | Servlet 6.1 / Jakarta EE 11 requires Jetty 12.1.x |

### 5.2 Forced consequences of the majors

Spring Boot 4 / Spring Framework 7 is a generational reset. Known impacts on this codebase:

1. **Jakarta EE 11 / Servlet 6.1 baseline.** `lumina-web` embeds Jetty directly. Jetty 12.0.x is
   Servlet 6.0 (EE10); **must move to Jetty 12.1.x** for Servlet 6.1. Verify the embedded server
   bootstrap and WebSocket API (`org.eclipse.jetty.ee11.*` vs `ee10`) still compile and the
   integration tests pass.
2. **Spring AI 1.1 → 2.0 API drift.** `SpringAiChatClient` and `LuminaSpringAiAutoConfiguration`
   use `org.springframework.ai.chat.client.ChatClient`, `ChatModel`, `ChatClient.create(ChatModel)`,
   and reactive `Flux<String>` streaming (`.stream().content()`). These package names / method
   signatures must be re-verified against the 2.0.0 API and fixed where they moved.
3. **Auto-configuration registration.** Confirm Boot 4's auto-configuration import file
   (`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`) location and
   format still apply to `lumina-spring-ai`; adjust if Boot 4 modularization changed it.
4. **Jackson 2 vs 3.** Boot 4 defaults to Jackson 3 (`tools.jackson.*`). `lumina-web`'s
   `ProtocolCodec` pins Jackson 2 (`com.fasterxml.jackson.*`) via its own `jackson-bom` import and is
   **not** a Spring module, so Jackson 2 stays isolated there. The boundary is documented; no forced
   migration of `lumina-web` unless a conflict surfaces. Spring modules (`lumina-spring-boot-starter`,
   `lumina-spring-ai`) inherit Boot 4's Jackson 3.
5. **Undertow dropped** in Boot 4 — not used by Lumina; no action.
6. **Removed 3.x deprecations** — audit `lumina-spring-boot-starter` (`LuminaAutoConfiguration`,
   `LuminaProperties`) for any removed Actuator/config-property APIs.

### 5.3 Upgrade acceptance

`mvn -q clean verify` passes for the whole reactor on JDK 25, including the `lumina-web`
integration tests and the `lumina-spring-ai` context tests. No provider or reactive dependency leaks
into `lumina-core`/`lumina-runtime` (unchanged from Phase 2).

## 6. Vision & Architecture document contents

### 6.1 `docs/VISION.md`

- Problem & thesis: pure-Java, zero HTML/CSS/JS interactive apps; AI-native by default.
- Audience & non-audience; positioning vs Streamlit / Vaadin / web MVC (what Lumina is **not**).
- The Phase 0–10 roadmap (§3) and the status matrix (§4).
- Success criteria & guiding principles (composition over inheritance, minimal client, SPI seams,
  Clean Architecture, semver, binary compatibility pre-1.0 caveats).

### 6.2 `docs/ARCHITECTURE.md`

Authoritative technical doc. Sections:

1. **C4 diagrams** (Mermaid): System Context, Container, Component.
2. **Module boundaries** — the 10 modules, their responsibilities, and the dependency rule
   (`API → Service/Runtime → Domain ← Infrastructure`; `lumina-core` depends on nothing internal;
   reuse ADR-001).
3. **Component model** — nested `ComponentNode` tree, stable keys, layout containers (ADR-007).
4. **Rendering engine** — server-side tree build → diff → patch/stream frames; thin client contract.
5. **State management** — typed session-scoped state; lifecycle; eviction/TTL hooks (ADR-008/010).
6. **Routing** — server-side `path → view`; URL/query as addressable state (ADR-008).
7. **Transport protocol** — WebSocket today; SSE fallback + reconnect/resume as SPI (ADR-012);
   reuse ADR-003 (wire protocol) + ADR-006 (streaming).
8. **AI seam** — `ChatClient`/`TokenStream` neutral SPI + optional capability SPIs (ADR-011).
9. **Extensibility SPIs** — component registry, AI-provider, transport, rendering, theme (ADR-009).
10. **Security model** — CSWSH/origin (exists), auth/SSO + RBAC hook points, session-id entropy,
    upload limits, session TTL/eviction (ADR-010).
11. **Concurrency model** — serial-per-session on virtual threads (reuse ADR-002).
12. **Performance goals** (§7).
13. **Non-functional requirements** (§7).
14. **Tech stack** — the verified §5 versions.

## 7. Performance goals & NFRs (proposed, benchmarked in P10)

Draft targets — testable in Phase 10, adjustable:

| Metric | Target |
|--------|--------|
| p95 interaction round-trip (intent → patch applied), in-region, trivial rerun | < 100 ms |
| Concurrent active sessions per node (baseline, 2 vCPU / 2 GB) | ≥ 1,000 |
| Typical patch payload (single-widget change) | < 8 KB |
| Reconnect to interactive (fresh snapshot) | < 2 s |
| Streaming append frame overhead (per chunk, server-side) | < 1 ms |
| Cold start (embedded server ready) | < 3 s |

NFRs: backward-compatible wire protocol within a MINOR; binary-compatible public API within a MINOR
(pre-1.0 caveat documented); no unbounded per-session memory growth; all public API Javadoc'd;
zero user-authored HTML/CSS/JS; graceful degradation when a transport/provider capability is absent.

## 8. New ADRs

| ID | Title |
|----|-------|
| ADR-007 | Nested component tree & layout containers |
| ADR-008 | State model & server-side routing |
| ADR-009 | Extensibility SPIs (component/provider/transport/rendering/theme) |
| ADR-010 | Security & session lifecycle model |
| ADR-011 | AI capability SPIs (tools / embeddings-RAG / usage-cost) |
| ADR-012 | Transport evolution (WS + SSE fallback, reconnect/resume) |

Each ADR is a *decision of direction* at architecture depth (context, decision, consequences). None
mandates implementation in Phase 0; they set the contracts later phases build to. ADR-001…006 are
reused by reference and unchanged.

## 9. Non-goals (YAGNI for Phase 0)

- No feature implementation (layout, routing, hot reload, widgets, AI components) — those are P1+.
- No plugin SDK / packaging spec — Phase 8.
- No cluster/session-replication protocol — Phase 7 (only the SPI seam is acknowledged).
- No i18n or full theming spec — Phase 6/7 (only hook points named).
- No benchmark harness — Phase 10 (only the targets are set).
- No changes to `lumina-web`'s Jackson 2 usage beyond what the Jetty 12.1 move forces.

## 10. Deliverables checklist

- [ ] Platform upgrade: `pom.xml` versions (Java 25, Boot 4.1.0, Spring AI 2.0.0, Jetty 12.1.x)
- [ ] `lumina-web` Servlet 6.1 / Jetty 12.1 (`ee11`) compile + IT green
- [ ] `lumina-spring-ai` re-verified against Spring AI 2.0.0 API
- [ ] `lumina-spring-boot-starter` audited for removed Boot 3.x deprecations
- [ ] `mvn -q clean verify` green on JDK 25 across the reactor
- [ ] Version bump to `0.3.0-SNAPSHOT`
- [ ] `docs/VISION.md`
- [ ] `docs/ARCHITECTURE.md` (C4 + 14 sections)
- [ ] ADR-007…012
- [ ] README/module docs updated to reflect the new stack & roadmap

## 11. Next step

After approval, produce a detailed implementation plan (`docs/superpowers/plans/…`) via the
writing-plans skill, then execute with subagent-driven development. The plan sequences the platform
upgrade **before** the documentation so the docs describe the verified stack.
