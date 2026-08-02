# Lumina

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-25%2B-orange.svg)](pom.xml)

**Lumina** is an open-source Java framework for interactive, server-driven web apps.
Inspired by Streamlit and built for modern Java and AI workloads, it owns rendering,
session state, and real-time UI updates so application code needs **no HTML, CSS, or JavaScript**.

**Status:** `1.0.0` — first community release (Apache-2.0). Build from source today;
Maven Central publish requires a Central Portal account for `io.lumina` (see [docs/RELEASING.md](docs/RELEASING.md)).

## Requirements

- **Java 25+** and **Maven 3.9+**
- Platform stack: Spring Boot **4.1.0**, Spring AI **2.0.0**, Jetty **12.1.11** (EE11)

## Maven coordinates

After Central publish (or from a local `mvn install`):

```xml
<dependency>
  <groupId>io.lumina</groupId>
  <artifactId>lumina-web</artifactId>
  <version>1.0.0</version>
</dependency>
```

Optional: `lumina-spring-boot-starter`, `lumina-spring-ai`, `lumina-cli`.

## Showcase quickstart

```bash
git clone https://github.com/twangdi07/lumina.git
cd lumina
mvn -q -pl lumina-examples -am install
mvn -q -pl lumina-examples exec:java -Dexec.mainClass=io.lumina.examples.showcase.ShowcaseMain
```

Open [http://127.0.0.1:8080](http://127.0.0.1:8080) for the enterprise shell, routing,
widgets, and AI demos (`/`, `/about`, `/widgets`, `/ai`).

The server binds to `127.0.0.1` by default. Use
`LuminaServerConfig.builder().host("0.0.0.0")...` to expose it on the network.

## Hello AI

Minimal stateful chat with the built-in offline echo client:

```bash
mvn -q -pl lumina-examples -am install
mvn -q -pl lumina-examples exec:java
```

Default entry point: `io.lumina.examples.helloai.HelloAiMain`.

## Streaming chat

```bash
mvn -q -pl lumina-examples -am install
mvn -q -pl lumina-examples exec:java -Dexec.mainClass=io.lumina.examples.streaming.StreamingChatMain
```

See [`lumina-examples/README.md`](lumina-examples/README.md#streaming-chat).

## Docs

- [Product overview](docs/PRODUCT.md)
- [Author guide](docs/GUIDE.md)
- [Vision and roadmap](docs/VISION.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Extensions](docs/EXTENSIONS.md)
- [Migration](docs/MIGRATION.md)
- [Releasing / Maven Central](docs/RELEASING.md)
- [Contributing](CONTRIBUTING.md)
- [Security](SECURITY.md)
- [Changelog](CHANGELOG.md)

## Modules

| Module | Role |
|--------|------|
| `lumina-core` | App, UI, state, model, AI, and transport contracts |
| `lumina-session` | Isolated session and widget state |
| `lumina-components` | Built-in component property definitions |
| `lumina-runtime` | Binding, execution, tree diffing |
| `lumina-web` | Jetty server, WebSocket, wire protocol, thin client |
| `lumina-devtools` | File-watch reload |
| `lumina-spring-boot-starter` | Optional Spring Boot auto-configuration |
| `lumina-spring-ai` | Spring AI `ChatClient` → `TokenStream` bridge |
| `lumina-cli` | CLI launcher for `LuminaApp` |
| `lumina-examples` | Showcase, Hello AI, streaming chat, agent demo |

## 1.0 limitations

- Multi-node session clustering is not shipped.
- SSE as an alternate transport is not shipped.
- Cloud LLM coverage depends on Spring AI configuration; echo works offline.

## License

Copyright © Lumina contributors. Licensed under the [Apache License 2.0](LICENSE).
