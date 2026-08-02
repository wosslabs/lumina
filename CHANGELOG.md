# Changelog

## 1.0.0 — 2026-08-02

First community release (Apache-2.0).

- Community packaging: `LICENSE`, `NOTICE`, `SECURITY.md`, `CODE_OF_CONDUCT.md`,
  GitHub issue/PR templates, expanded README/GUIDE/CONTRIBUTING, `docs/RELEASING.md`.
- Parent POM release metadata (`url`, `licenses`, `scm`, `developers`) and `-Prelease`
  profile (sources, javadoc, GPG, Central Publishing plugin).
- P2 core widgets, download support, and showcase coverage.
- P3 AI metadata cards for citations, RAG sources, tool calls, and usage.
- P4 provider SPI and Spring AI bridge properties.
- P5 agent timeline, approvals, tool invocation, and memory panels.
- P6 tabs, dialogs, notifications, themes, and responsive sidebar behavior.
- P7 role-gating, audit SPI, auth context, and message lookup hooks.
- P8 extension and theme SPIs.
- P9 file-watch development reload and JDK 25 CI.
- P1 routing (`ui.path` / `ui.navigate`) and P1.5 enterprise shell UX hard reset.
- Blank-page fixes for layout CSS selectors and custom-element registration.
- Maven Central `groupId` set to `io.github.wosslabs` (GitHub-verified namespace).
  Java packages remain `io.lumina.*`. `lumina-examples` is not published (`maven.deploy.skip`).

### Known limitations

- Claim/verify `io.github.wosslabs` in Central Portal before `mvn -Prelease deploy`.
- No multi-node session clustering; no SSE transport; cloud providers via Spring AI only.
