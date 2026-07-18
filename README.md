# Lumina

Lumina is an open-source Java framework for building interactive, server-driven
web applications in pure Java. Inspired by Streamlit and designed for modern
Java and AI applications, it owns rendering, session state, and real-time UI
updates so application code needs no HTML, CSS, or JavaScript.

Lumina Phase 1 requires Java 21 and Maven.

## Hello AI quickstart

From the repository root, build and run the included Hello AI application:

```bash
mvn -q -pl lumina-examples -am install
mvn -q -pl lumina-examples exec:java
```

Open [http://localhost:8080](http://localhost:8080), enter a prompt, and the
built-in offline chat client will echo it. The example entry point is
`io.lumina.examples.helloai.HelloAiMain`.

The two commands are intentionally separate: the first builds and installs the
example and its reactor dependencies into the local Maven repository (required
on a fresh checkout so sibling SNAPSHOT artifacts resolve), while the second
runs the example module's configured main class.

## Modules

- `lumina-core` — application, UI, state, model, AI, and transport contracts.
- `lumina-session` — isolated session and widget state storage.
- `lumina-components` — shared built-in component property definitions.
- `lumina-runtime` — UI binding, session execution, and component-tree diffing.
- `lumina-web` — embedded Jetty server, WebSocket endpoint, and wire protocol.
- `lumina-devtools` — development reload SPI and Phase 1 no-op implementation.
- `lumina-spring-boot-starter` — optional Spring Boot auto-configuration.
- `lumina-cli` — command-line launcher for `LuminaApp` implementations.
- `lumina-examples` — runnable applications, including Hello AI.

## Design

The approved [Phase 1 architecture design](docs/superpowers/specs/2026-07-18-lumina-phase1-design.md)
documents the goals, non-goals, architecture, protocol, and ADRs behind the
implementation.
