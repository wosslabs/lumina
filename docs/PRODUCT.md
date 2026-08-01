# Lumina Product Overview

**Lumina** is an open-source Java framework for building interactive, AI-native web apps in pure Java — with zero author-written HTML, CSS, or JavaScript.

## Quick start

```bash
mvn -q -pl lumina-examples -am install
mvn -q -pl lumina-examples exec:java -Dexec.mainClass=io.lumina.examples.showcase.ShowcaseMain
# open http://127.0.0.1:8080/
```

## Who it’s for

Java / Spring teams who want Streamlit-like productivity on the JVM.

## Core ideas

1. **Declare UI in Java** — `LuminaApp.build(Ui ui)`
2. **Rerun on interaction** — server rebuilds the component tree and patches the client
3. **Thin client** — framework-owned Web Components + design system
4. **AI-native** — chat, streaming, and agent surfaces are first-class

## Shipped feature map (1.0 MVP)

| Area | Capabilities |
|------|----------------|
| Kernel | Jetty server, sessions, WebSocket, routing, nested layout |
| UX | Enterprise shell, tokens, a11y constitution, theme toggle |
| Widgets | Text, markdown, inputs, select/radio/slider, tables, files, download, spinner |
| AI | Chat, streaming, citations, RAG sources, tool calls, usage metrics |
| Agents | Timeline, approvals, tool rows, memory panel |
| Platform | Spring Boot starter, Spring AI bridge, provider SPI, plugins |
| Enterprise | Auth hooks, RBAC helpers, metrics, audit SPI, i18n hooks |
| DX | CLI, hot reload, CI, docs, 1.0 packaging |

## Learn more

- [Vision & roadmap](VISION.md)
- [Architecture](ARCHITECTURE.md)
- [Author guide](GUIDE.md)
- [Extensions](EXTENSIONS.md)
- [Migration to 1.0](MIGRATION.md)
- [UX constitution checklist](superpowers/specs/2026-08-01-lumina-ux-constitution-checklist.md)
