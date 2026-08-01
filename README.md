# Lumina

Lumina is an open-source Java framework for building interactive, server-driven
web applications in pure Java. Inspired by Streamlit and designed for modern
Java and AI applications, it owns rendering, session state, and real-time UI
updates so application code needs no HTML, CSS, or JavaScript.

## Requirements

- **Java 25+** and **Maven 3.9+**
- Platform stack: Spring Boot **4.1.0**, Spring AI **2.0.0**, Jetty **12.1.11** (EE11)

## Product and roadmap

- [Product overview](docs/PRODUCT.md)
- [Vision and shipped roadmap](docs/VISION.md)
- [Author guide](docs/GUIDE.md)
- [Extension guide](docs/EXTENSIONS.md)
- [Migration notes](docs/MIGRATION.md)
- [Contributing](CONTRIBUTING.md)

## Architecture

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the current system
architecture, module boundaries, and design decisions.

## Showcase quickstart

From the repository root, build and run the showcase:

```bash
mvn -q -pl lumina-examples -am install
mvn -q -pl lumina-examples exec:java -Dexec.mainClass=io.lumina.examples.showcase.ShowcaseMain
```

Open [http://127.0.0.1:8080](http://127.0.0.1:8080) for the enterprise shell, routing,
widgets, and AI demos (`/`, `/about`, `/widgets`, `/ai`).

By default the embedded server binds to `127.0.0.1` (loopback only) so it is
not reachable from other machines on the network. Pass a
`LuminaServerConfig.builder().host("0.0.0.0")...` (or another address) to
`LuminaServer.start(app, config)` to expose it more broadly.

The two commands are intentionally separate: the first builds and installs the
example and its reactor dependencies into the local Maven repository (required
on a fresh checkout so sibling SNAPSHOT artifacts resolve), while the second
runs the showcase main class.

## Hello AI

Minimal stateful chat using the built-in offline echo client:

```bash
mvn -q -pl lumina-examples -am install
mvn -q -pl lumina-examples exec:java
```

Enter a prompt and the echo client replies. Default entry point:
`io.lumina.examples.helloai.HelloAiMain`.

## Streaming chat

`StreamingChatApp` in `lumina-examples` demonstrates `Ui.ai(TokenStream)`:
replies stream to the client as they are produced instead of appearing all at
once. See [`lumina-examples/README.md`](lumina-examples/README.md#streaming-chat)
for details.

```bash
mvn -q -pl lumina-examples -am install
mvn -q -pl lumina-examples exec:java -Dexec.mainClass=io.lumina.examples.streaming.StreamingChatMain
```

## Modules

- `lumina-core` — application, UI, state, model, AI, and transport contracts.
- `lumina-session` — isolated session and widget state storage.
- `lumina-components` — shared built-in component property definitions.
- `lumina-runtime` — UI binding, session execution, and component-tree diffing.
- `lumina-web` — embedded Jetty server, WebSocket endpoint, and wire protocol.
- `lumina-devtools` — development reload SPI and Phase 1 no-op implementation.
- `lumina-spring-boot-starter` — optional Spring Boot auto-configuration.
- `lumina-spring-ai` — optional Spring AI `ChatClient` adapter that streams
  replies through `TokenStream` via a Reactor `Flux` bridge.
- `lumina-cli` — command-line launcher for `LuminaApp` implementations.
- `lumina-examples` — runnable applications, including the P1.5 showcase,
  Hello AI, and the streaming chat example.

## Design

The approved [Phase 1 architecture design](docs/superpowers/specs/2026-07-18-lumina-phase1-design.md)
documents the goals, non-goals, architecture, protocol, and ADRs behind the
implementation.
