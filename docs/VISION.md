# Lumina Vision

## Thesis

Lumina is an open-source Java framework for building interactive, AI-native web applications in **pure Java** — with **zero user-authored HTML, CSS, or JavaScript**. Developers express UI and behavior in Java; the framework owns the thin client, server-driven component tree, diff/patch protocol, and session lifecycle. AI is a first-class concern by default: chat, streaming tokens, and provider integration are part of the product shape, not bolted-on afterthoughts.

## Audience / non-audience and positioning

**Audience.** Java developers who want Streamlit-like productivity for interactive and AI-driven apps without leaving the JVM — especially teams already on Spring Boot / Spring AI who want a coherent UI + AI stack in one language.

**Non-audience.** Front-end specialists building custom SPAs; teams that need pixel-perfect design systems authored in HTML/CSS/JS; projects whose primary goal is a traditional multi-page MVC site or a full Vaadin/React replacement with client-side routing and rich client logic.

**What Lumina is not.** Lumina is **not** a web MVC framework, **not** a Vaadin or React wrapper, and **not** a thin veneer over user-authored front-end code. There is no expectation that application authors write or ship HTML/CSS/JS.

**Positioning vs Streamlit.** Like Streamlit, Lumina optimizes for fast, script-like app construction: declare UI in the host language, rerun on interaction, keep state on the server. Unlike Streamlit (Python + its own runtime), Lumina is Java-native, embeds in the JVM (Jetty today), integrates with Spring Boot and Spring AI when desired, and keeps a hard invariant of zero user-authored web markup. The client stays thin and build-step-free; the server owns the component tree and patches.

## Roadmap

Canonical Phase 0–10 roadmap:

- **P0 Vision & Architecture** — this spec's deliverables.
- **P1 Framework Kernel (MVP)** — embedded server, component tree, rendering, diff engine, session
  state, routing, basic layout, hot reload, WebSocket/SSE transport, Hello World.
- **P1.5 UX Foundation** — design system (tokens, typography, dark mode baseline), app shell
  (wide/centered layout, styled sidebar), widget polish, `pageConfig` API, showcase demo.
- **P2 Core UI Components** — text, markdown, code, buttons, text/number input, checkbox, select,
  radio, slider, images, tables, JSON viewer, progress, spinners, file upload/download.
- **P3 AI Components** — chat UI, streaming tokens, prompt editor, history, markdown, code
  highlighting, citations, RAG sources, tool-call visualization, token usage, cost, latency.
- **P4 Spring AI & LangChain4j Integration** — auto-config + first-class ChatClient/ChatModel;
  Bedrock, OpenAI, Azure, Vertex, Ollama; `ui.ai(chatClient.prompt(prompt))`.
- **P5 Agentic UI** — agent timeline, tool invocation viewer, reasoning graph, workflow viz, memory
  inspection, multi-agent, human approval steps, live execution logs.
- **P6 Advanced Layout & UX** — tabs, dialogs, notifications, responsive breakpoints, theme toggle,
  accessibility, keyboard shortcuts (baseline visual polish moved to P1.5).
- **P7 Enterprise** — auth/SSO hooks, RBAC, session clustering, observability
  (Micrometer/OpenTelemetry), audit logging, i18n, config profiles, security hardening.
- **P8 Extensibility** — plugin API, component SDK, theme SDK, transport SPI, rendering SPI, AI
  provider SPI, packaging & extension guides.
- **P9 Developer Experience** — Spring Boot starter, CLI (new/run/package/deploy), hot reload, live
  preview, starter templates, samples, Maven Central publishing, GitHub Actions, docs site.
- **P10 1.0 Release** — API freeze, semver, binary-compat checks, benchmarks, docs, migration guide,
  contribution guidelines, future roadmap.

## Status matrix

Reconciliation of the roadmap with shipped code.

Legend: ✅ done · ◐ partial · ❌ not started

| Phase | Item | Status | Notes |
|-------|------|--------|-------|
| P1 | Embedded server | ✅ | Jetty 12 embedded (`lumina-web`, ADR-005) |
| P1 | Component tree | ✅ | Nested `ComponentNode` tree (ADR-007, `0.4.0`) |
| P1 | Rendering engine | ✅ | Server-driven nested layout + diff patches |
| P1 | Diff engine | ✅ | `TreeDiffer` + patch ops (ADR-003) |
| P1 | Session state | ◐ | `StateStore` + `WidgetState`; not typed |
| P1 | Routing | ✅ | `ui.path()` / `ui.navigate()`; `__lumina.path` in session store (`0.6.0`) |
| P1 | Basic layout | ✅ | `container`, `columns`, `sidebar`, `expander` (`0.4.0`) |
| P1 | Hot reload | ❌ | `NoOpReloader` stub only |
| P1 | WebSocket transport | ✅ | `LuminaWebSocketEndpoint` + backpressure |
| P1 | SSE transport | ❌ | — |
| P1 | Hello World | ✅ | `lumina-examples` |
| P1.5 | Design system / tokens | ✅ | `lumina-tokens.css`, ADR-013 (`0.5.0`) |
| P1.5 | App shell (wide/centered) | ✅ | `PageConfig` API + client shell CSS |
| P1.5 | Widget visual polish | ✅ | All existing types restyled |
| P1.5 | Dark mode baseline | ✅ | `prefers-color-scheme` tokens |
| P1.5 | Showcase demo | ✅ | `ShowcaseApp` hero demo |
| P2 | text/markdown/code/button/text-input/table/json/image/progress/file-upload | ✅ | `lumina-components` — P1.5 polish applied |
| P2 | number/checkbox/select/radio/slider/spinner/download | ❌ | Next P2 widgets |
| P3 | chat UI, streaming tokens, history, markdown, code highlight | ✅ | Phase 2 delivered |
| P3 | citations, RAG sources, tool-call viz, token usage, cost, latency | ❌ | — |
| P4 | Spring AI ChatClient auto-config | ◐ | `lumina-spring-ai`; echo-tested only, no real model wired |
| P4 | LangChain4j, Bedrock, OpenAI, Azure, Vertex, Ollama | ❌ | — |
| P5–P10 | all | ❌ | Not started |

**Resume point:** P1 gaps (hot reload, SSE) → P2 widgets.

## Guiding principles

1. **Composition over inheritance.** Prefer composing small, reusable pieces (components, SPIs, adapters) over deep class hierarchies.
2. **Thin client.** The browser client stays build-step-free (vanilla Web Components + server-driven tree diff). Zero user-authored HTML/CSS/JS is a hard invariant; the client grows only for layout and theming needs.
3. **SPI seams.** Core depends on narrow interfaces for component registry, AI providers, transport, rendering, and theme so later phases (clustering, plugins, alternate transports) slot in without rewriting the kernel.
4. **Clean Architecture / SOLID.** Dependency rule: API → Service/Runtime → Domain ← Infrastructure. `lumina-core` depends on nothing Lumina-internal; Spring, Jetty, and provider SDKs stay at the edges (`lumina-web`, `lumina-spring-boot-starter`, `lumina-spring-ai`, etc.).
5. **Semver with a pre-1.0 caveat.** Public APIs follow semantic versioning intent; before 1.0, binary compatibility within a MINOR is a goal but not a hard guarantee — breaking changes may still land when the architecture demands it. Wire protocol stays backward-compatible within a MINOR where possible.
6. **Virtual Threads.** Concurrency is serial-per-session on virtual threads: simple mental model for authors, scalable concurrency for the runtime.
7. **Minimal-dependency core.** `lumina-core`, `lumina-session`, `lumina-components`, and `lumina-runtime` stay free of Spring, Jetty, and Servlet APIs. Optional modules (`lumina-web`, `lumina-devtools`, `lumina-spring-boot-starter`, `lumina-spring-ai`, `lumina-cli`, `lumina-examples`) adopt platform concerns at the boundary.
