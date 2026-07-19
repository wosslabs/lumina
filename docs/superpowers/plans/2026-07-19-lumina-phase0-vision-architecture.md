# Lumina Phase 0 — Vision & Architecture (+ Platform Upgrade) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade the platform to Java 25 / Spring Boot 4.1.0 / Spring AI 2.0.0 / Jetty 12.1.11, then publish the canonical Lumina vision and architecture documentation (VISION, ARCHITECTURE, ADR-007…012) describing the verified stack and the adopted Phase 0–10 roadmap.

**Architecture:** Two sequential task groups. Group A performs the platform upgrade with `mvn` gates so the docs describe a real, building stack. Group B writes documentation only (no code). The single hard code change is migrating `lumina-web` from Jetty EE10 to EE11 (Servlet 6.1); everything else is version bumps + API re-verification.

**Tech Stack:** Java 25, Spring Boot 4.1.0 (`spring-boot-dependencies` BOM), Spring AI 2.0.0 (`spring-ai-bom`), Jetty 12.1.11 (`jetty-bom`, EE11 servlet + websocket), Jackson 2 (isolated in `lumina-web`), JUnit 6.0.3, AssertJ, Maven, Mermaid C4.

## Global Constraints

- Spec: `docs/superpowers/specs/2026-07-19-lumina-phase0-vision-architecture-design.md`.
- Java baseline: **25** (`java.version` + `maven.compiler.release` = `25`). Build/test on JDK 25.
- Versions are exact and verified (2026-07-18): Spring Boot **4.1.0**, Spring AI **2.0.0**, Jetty **12.1.11**. Do NOT substitute other versions; if any fails to resolve, stop and report rather than guess.
- Project version bumps to **`0.3.0-SNAPSHOT`** (all module POMs + parent).
- `lumina-core` and `lumina-runtime` stay provider-free and reactive-free (unchanged from Phase 2).
- Zero user-authored HTML/CSS/JS invariant is unchanged.
- Reuse ADR-001…006 by reference; do not rewrite them.
- Git policy (repo rule): the **developer commits manually**; never skip hooks. Commit steps below describe the intended unit of work — when an agent executes, it stages and surfaces the diff for the developer to commit unless the developer explicitly authorizes committing.
- No feature code (layout, routing, hot reload, widgets, AI components) in Phase 0 — docs + upgrade only.

---

# Group A — Platform Upgrade

### Task 1: [Upgrade] Bump parent versions and verify pure-Java modules on JDK 25

**Files:**
- Modify: `pom.xml:23-33` (properties block)

**Interfaces:**
- Produces: parent `dependencyManagement` now imports Spring Boot 4.1.0 + Spring AI 2.0.0 BOMs and Jetty 12.1.11 BOM; `release=25`. Consumed by every module task below.

- [ ] **Step 1: Edit the properties block**

Replace the version properties in `pom.xml`:

```xml
<properties>
  <java.version>25</java.version>
  <maven.compiler.release>25</maven.compiler.release>
  <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  <junit.version>5.11.4</junit.version>
  <assertj.version>3.27.3</assertj.version>
  <jetty.version>12.1.11</jetty.version>
  <jackson.version>2.18.3</jackson.version>
  <spring-boot.version>4.1.0</spring-boot.version>
  <spring-ai.version>2.0.0</spring-ai.version>
</properties>
```

- [ ] **Step 2: Confirm the toolchain is JDK 25**

Run: `java -version && mvn -version`
Expected: runtime Java version reports `25`. If not, install/select JDK 25 before continuing (e.g. via SDKMAN) — do not proceed on an older JDK.

- [ ] **Step 3: Build + test the non-Spring, non-Jetty modules**

Run: `mvn -q -pl lumina-core,lumina-session,lumina-components,lumina-runtime -am clean test`
Expected: BUILD SUCCESS; all Phase 1/2 unit tests pass unchanged on JDK 25.

- [ ] **Step 4: Commit**

```bash
git add pom.xml
git commit -m "build: target Java 25 and Spring Boot 4.1 / Spring AI 2.0 / Jetty 12.1 BOMs"
```

---

### Task 2: [Upgrade] Migrate `lumina-web` from Jetty EE10 to EE11 (Servlet 6.1)

**Files:**
- Modify: `lumina-web/pom.xml:27-34` (Jetty artifacts)
- Modify: `lumina-web/src/main/java/io/lumina/web/JettyLuminaHttpServer.java:9-14` (imports)
- Test: existing `lumina-web/src/test/java/.../LuminaServerIT.java` (unchanged; must stay green)

**Interfaces:**
- Consumes: Jetty 12.1.11 from Task 1's `jetty-bom`.
- Produces: `LuminaServer.start(...)`, `JettyLuminaHttpServer` unchanged in signature; only Jetty package coordinates change. `LuminaWebSocketEndpoint` (uses version-neutral `org.eclipse.jetty.websocket.api.*`) is unchanged.

- [ ] **Step 1: Swap the EE10 Jetty artifacts for EE11 in `lumina-web/pom.xml`**

```xml
<dependency>
  <groupId>org.eclipse.jetty.ee11</groupId>
  <artifactId>jetty-ee11-servlet</artifactId>
</dependency>
<dependency>
  <groupId>org.eclipse.jetty.ee11.websocket</groupId>
  <artifactId>jetty-ee11-websocket-jetty-server</artifactId>
</dependency>
```

(`org.eclipse.jetty:jetty-server` dependency stays as-is.)

- [ ] **Step 2: Update the EE10 imports to EE11 in `JettyLuminaHttpServer.java`**

```java
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.ee11.websocket.server.JettyServerUpgradeRequest;
import org.eclipse.jetty.ee11.websocket.server.JettyServerUpgradeResponse;
import org.eclipse.jetty.ee11.websocket.server.JettyWebSocketServerContainer;
import org.eclipse.jetty.ee11.websocket.server.config.JettyWebSocketServletContainerInitializer;
```

Leave `org.eclipse.jetty.server.Server` / `ServerConnector` imports unchanged.

- [ ] **Step 3: Compile and check for API drift**

Run: `mvn -q -pl lumina-web -am clean test-compile`
Expected: compiles. If any EE11 type moved or a method signature changed (e.g. `ServletContextHandler` constructor, `JettyWebSocketServletContainerInitializer.configure`), fix the call site to the 12.1 API — do not downgrade Jetty. Read the 12.1 programming guide if a symbol is missing.

- [ ] **Step 4: Run the web integration tests**

Run: `mvn -q -pl lumina-web -am test -Dsurefire.failIfNoSpecifiedTests=false`
Expected: `LuminaServerIT` (HTTP + WebSocket handshake, snapshot/patch, streaming frames) passes against Jetty 12.1 EE11.

- [ ] **Step 5: Commit**

```bash
git add lumina-web/pom.xml lumina-web/src/main/java/io/lumina/web/JettyLuminaHttpServer.java
git commit -m "build: migrate lumina-web to Jetty 12.1 EE11 (Servlet 6.1)"
```

---

### Task 3: [Upgrade] Re-verify `lumina-spring-ai` against Spring AI 2.0.0

**Files:**
- Modify (if API drifted): `lumina-spring-ai/src/main/java/io/lumina/springai/SpringAiChatClient.java`
- Modify (if API drifted): `lumina-spring-ai/src/main/java/io/lumina/springai/LuminaSpringAiAutoConfiguration.java`
- Verify: `lumina-spring-ai/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Modify: `lumina-spring-ai/pom.xml` (only if a dependency coordinate changed in 2.0)
- Test: existing `lumina-spring-ai` tests (Flux→TokenStream bridge, auto-config context)

**Interfaces:**
- Consumes: Spring AI 2.0.0 (`org.springframework.ai.chat.client.ChatClient`, `org.springframework.ai.chat.model.ChatModel`), Spring Boot 4.1.0 auto-config.
- Produces: `SpringAiChatClient implements io.lumina.ai.ChatClient` and `luminaSpringAiChatClient(ChatModel)` bean — signatures unchanged for downstream consumers.

- [ ] **Step 1: Compile against Spring AI 2.0.0**

Run: `mvn -q -pl lumina-spring-ai -am clean test-compile`
Expected: compiles. If `ChatClient.create(ChatModel)`, `ChatModel`, or the streaming call (`chatClient.prompt().user(input).stream().content()` returning `reactor.core.publisher.Flux<String>`) moved packages or changed signatures in 2.0, update the imports/calls to the 2.0 API. Preserve the blocking `Flux<String>` → `TokenStream` bridge (bounded `BlockingQueue` + end/error sentinel) semantics; only adjust the reactive source call if its shape changed.

- [ ] **Step 2: Verify the auto-configuration registration file still applies**

Confirm `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` is still the Boot 4 discovery mechanism and lists `io.lumina.springai.LuminaSpringAiAutoConfiguration`. If Boot 4 modularization relocated the file, move it accordingly and note it in the ADR/README. Confirm `@AutoConfiguration`, `@ConditionalOnClass(ChatClient.class)`, `@ConditionalOnBean(ChatModel.class)`, `@ConditionalOnMissingBean(io.lumina.ai.ChatClient.class)` still resolve.

- [ ] **Step 3: Run the module tests**

Run: `mvn -q -pl lumina-spring-ai -am test -Dsurefire.failIfNoSpecifiedTests=false`
Expected: Flux→TokenStream bridge unit test and the auto-config context test pass. If a test uses a Spring AI 1.x test type that moved, update the test import only (no behavior change).

- [ ] **Step 4: Commit**

```bash
git add lumina-spring-ai
git commit -m "build: verify lumina-spring-ai against Spring AI 2.0.0"
```

---

### Task 4: [Upgrade] Audit `lumina-spring-boot-starter` for removed Boot 3.x APIs

**Files:**
- Modify (if needed): `lumina-spring-boot-starter/src/main/java/io/lumina/spring/LuminaAutoConfiguration.java`
- Modify (if needed): `lumina-spring-boot-starter/src/main/java/io/lumina/spring/LuminaProperties.java`
- Verify: the starter's `AutoConfiguration.imports` file

**Interfaces:**
- Consumes: Spring Boot 4.1.0.
- Produces: the starter's public bean/property surface unchanged unless a Boot API was removed.

- [ ] **Step 1: Compile the starter against Boot 4.1**

Run: `mvn -q -pl lumina-spring-boot-starter -am clean test-compile`
Expected: compiles. If a removed 3.x API surfaces (legacy config-property binding, removed `@ConfigurationProperties` helper, Actuator API), migrate to the Boot 4 equivalent named in the Spring Boot 4.0 migration guide. If nothing changed, this task is a no-op confirming compatibility.

- [ ] **Step 2: Run the starter tests**

Run: `mvn -q -pl lumina-spring-boot-starter -am test -Dsurefire.failIfNoSpecifiedTests=false`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit (only if files changed)**

```bash
git add lumina-spring-boot-starter
git commit -m "build: align lumina-spring-boot-starter with Spring Boot 4.1"
```

---

### Task 5: [Upgrade] Full-reactor verify on JDK 25 + version bump to 0.3.0-SNAPSHOT

**Files:**
- Modify: `pom.xml` (parent `junit.version` property)
- Modify: `lumina-spring-boot-starter/pom.xml` (remove the module-local `junit-bom` override added in Task 2ee5831)
- Modify: every module `pom.xml` `<version>`/parent `<version>` `0.2.0-SNAPSHOT` → `0.3.0-SNAPSHOT`

**Interfaces:**
- Consumes: Tasks 1–4.
- Produces: a fully building `0.3.0-SNAPSHOT` reactor on the new platform.

- [ ] **Step 1: Align JUnit at the parent level (consolidate Task 4's module-local override)**

Task 4 found Spring Boot 4.1 requires JUnit Jupiter 6.x for `SpringExtension` and applied a module-local `junit-bom` override in `lumina-spring-boot-starter/pom.xml`. Consolidate it:
- In parent `pom.xml`, bump `<junit.version>` from `5.11.4` to the JUnit version Spring Boot 4.1.0 manages (Task 4 verified **6.0.3**; re-confirm it matches `spring-boot-dependencies` 4.1.0's managed `junit-jupiter` version before setting). The parent already imports `junit-bom` via `${junit.version}` ahead of `spring-boot-dependencies`, so this one property governs all modules.
- Remove the now-redundant module-local `junit-bom` `dependencyManagement` override from `lumina-spring-boot-starter/pom.xml` (added in commit `2ee5831`) so there is a single JUnit source of truth.

Run: `mvn -q -pl lumina-spring-boot-starter,lumina-core,lumina-session,lumina-runtime -am test -Dsurefire.failIfNoSpecifiedTests=false`
Expected: tests pass with JUnit 6.0.3 supplied by the parent (no module-local override). If any module's existing test uses a JUnit API changed in 6.x, fix the test at its source.

- [ ] **Step 2: Bump the version across the reactor**

Run: `mvn -q versions:set -DnewVersion=0.3.0-SNAPSHOT -DgenerateBackupPoms=false`
Expected: all module POMs (parent + 10 modules) now read `0.3.0-SNAPSHOT`. Verify none were missed with `grep -rn "0.2.0-SNAPSHOT" --include=pom.xml .` (expect no matches).

- [ ] **Step 3: Clean verify the whole reactor**

Run: `mvn -q clean verify`
Expected: BUILD SUCCESS for all 10 modules on JDK 25, including the `lumina-web` IT and the `lumina-spring-ai` tests. Fix any residual failures at their source (do not skip tests).

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "build: bump Lumina to 0.3.0-SNAPSHOT on Java 25 / Spring Boot 4.1 / Spring AI 2.0"
```

---

# Group B — Vision & Architecture Documentation

> Documentation tasks: no unit tests. "Verification" = the doc exists, is internally consistent with the spec, all internal links/paths resolve, and Mermaid diagrams render (paste-check on mermaid.live or a Markdown preview). Draw all content from the approved spec §3–§8.

### Task 6: [Docs] `docs/VISION.md`

**Files:**
- Create: `docs/VISION.md`

- [ ] **Step 1: Write the vision document**

Include, in order:
1. **Thesis** — pure-Java, zero HTML/CSS/JS interactive apps; AI-native by default.
2. **Audience / non-audience** and **positioning** — explicitly what Lumina is NOT (not a web/MVC/Vaadin/React wrapper), contrasted with Streamlit's model.
3. **Roadmap** — the Phase 0–10 list verbatim from spec §3.
4. **Status matrix** — copy spec §4 (✅/◐/❌ table) and state the resume point (P1 gaps → P2 widgets).
5. **Guiding principles** — composition over inheritance, thin client, SPI seams, Clean Architecture/SOLID, semver + pre-1.0 binary-compat caveat, Virtual Threads, minimal-dependency core.

- [ ] **Step 2: Verify**

Confirm the roadmap and status matrix match spec §3/§4 exactly (no drift), and every module name matches the 10 real modules. Preview the Markdown to confirm the table renders.

- [ ] **Step 3: Commit**

```bash
git add docs/VISION.md
git commit -m "docs: add Lumina vision and Phase 0-10 roadmap with status matrix"
```

---

### Task 7: [Docs] `docs/ARCHITECTURE.md`

**Files:**
- Create: `docs/ARCHITECTURE.md`

**Interfaces:**
- Consumes: the verified stack from Group A (cite exact versions in the Tech Stack section).

- [ ] **Step 1: Write the C4 diagrams (Mermaid)**

Three fenced ```mermaid``` blocks:
- **System Context** — user/browser ↔ Lumina app ↔ AI provider(s).
- **Container** — browser thin client, embedded Jetty (`lumina-web`), runtime (`lumina-runtime`), core (`lumina-core`), session (`lumina-session`), components (`lumina-components`), Spring integration (`lumina-spring-boot-starter`, `lumina-spring-ai`).
- **Component** — inside a running session: `Ui` DSL → `UiBinder` → `ComponentNode` tree → `TreeDiffer` → transport (`RunSink`/WebSocket) → thin client.

- [ ] **Step 2: Write the 14 sections**

Follow spec §6.2 exactly — one section each: (1) C4 [from Step 1], (2) Module boundaries + dependency rule (reference ADR-001), (3) Component model (ADR-007), (4) Rendering engine, (5) State management (ADR-008/010), (6) Routing (ADR-008), (7) Transport (reference ADR-003/006; forward-reference ADR-012), (8) AI seam (ADR-011), (9) Extensibility SPIs (ADR-009), (10) Security model (ADR-010), (11) Concurrency (reference ADR-002), (12) Performance goals (copy spec §7 table), (13) NFRs (copy spec §7 NFR list), (14) Tech stack (the exact Group A versions: Java 25, Spring Boot 4.1.0, Spring AI 2.0.0, Jetty 12.1.11 EE11, Jackson 2 in `lumina-web`).

- [ ] **Step 3: Verify**

Every ADR reference (001–012) points to a real file (001–006 exist; 007–012 created in Task 8 — cross-check after Task 8). Performance/NFR content matches spec §7. Mermaid blocks render. Version numbers match Group A exactly.

- [ ] **Step 4: Commit**

```bash
git add docs/ARCHITECTURE.md
git commit -m "docs: add Lumina architecture (C4 + module/component/transport/AI/security model)"
```

---

### Task 8: [Docs] ADR-007…012

**Files:**
- Create: `docs/adr/ADR-007-nested-component-tree.md`
- Create: `docs/adr/ADR-008-state-and-routing.md`
- Create: `docs/adr/ADR-009-extensibility-spis.md`
- Create: `docs/adr/ADR-010-security-and-session-lifecycle.md`
- Create: `docs/adr/ADR-011-ai-capability-spis.md`
- Create: `docs/adr/ADR-012-transport-evolution.md`

**Interfaces:**
- Match the existing ADR style (see `docs/adr/ADR-006-streaming-protocol.md`): `# ADR-NNN: Title`, `## Status` (Accepted), `## Context`, `## Decision`, `## Consequences`.

- [ ] **Step 1: Write each ADR at architecture depth (direction, not implementation)**

- **ADR-007 Nested component tree & layout containers:** decide to evolve the flat child list into a nested `ComponentNode` tree with stable keys; layout containers (columns/tabs/sidebar/container/form) hold children; `TreeDiffer` reconciles nested trees; keying rules extend Phase 1 (ADR-004). Consequences: biggest kernel change, enables P1 layout + P6 advanced layout.
- **ADR-008 State model & server-side routing:** typed session-scoped state evolving `StateStore`; server-side `path → view` routing; URL/query as addressable state; interaction with rerun loop (ADR-002). Consequences: multi-page apps; state lifecycle owned by session.
- **ADR-009 Extensibility SPIs:** name the seams (component registry, AI-provider, transport, rendering, theme) and that they are internal-stable now, public plugin SDK deferred to P8. Consequences: core is plugin-ready without shipping the SDK.
- **ADR-010 Security & session lifecycle:** origin/CSWSH (exists), auth/SSO + RBAC hook points via Spring Security filter chain, session-id entropy, upload limits, session TTL/eviction. Consequences: enforcement built in P7; hooks designed now.
- **ADR-011 AI capability SPIs:** keep neutral `ChatClient`/`TokenStream` seam; add optional capability interfaces (tool-calling, embeddings/RAG, usage/cost); providers implement what they support; graceful degradation when absent. Consequences: multi-provider (P4) without leaking provider types into core.
- **ADR-012 Transport evolution:** WebSocket primary (ADR-005/006); SSE fallback; reconnect/resume via fresh snapshot; transport as an SPI for clustering (P7). Consequences: resilience path defined; single-node default unaffected.

- [ ] **Step 2: Verify**

Each ADR uses the ADR-006 heading structure, status `Accepted`, and stays at decision level (no code). No ADR mandates Phase 0 implementation.

- [ ] **Step 3: Commit**

```bash
git add docs/adr/ADR-007-*.md docs/adr/ADR-008-*.md docs/adr/ADR-009-*.md docs/adr/ADR-010-*.md docs/adr/ADR-011-*.md docs/adr/ADR-012-*.md
git commit -m "docs: add ADR-007..012 (component tree, state/routing, SPIs, security, AI, transport)"
```

---

### Task 9: [Docs] Update README/module docs for the new stack & roadmap

**Files:**
- Modify: `README.md` (root)
- Modify: `lumina-examples/README.md` (only if it names Java/Spring versions or run prerequisites)

- [ ] **Step 1: Update the root README**

- Update any prerequisite/version references to Java 25, Spring Boot 4.1.0, Spring AI 2.0.0, Jetty 12.1.11.
- Add a short "Roadmap" pointer linking to `docs/VISION.md` and "Architecture" pointer linking to `docs/ARCHITECTURE.md`.
- Confirm the quickstart command still works on JDK 25 (run it).

- [ ] **Step 2: Verify quickstart on the new platform**

Run: `mvn -q -pl lumina-examples -am install` then the documented run command.
Expected: the Hello AI / streaming example still launches on JDK 25.

- [ ] **Step 3: Commit**

```bash
git add README.md lumina-examples/README.md
git commit -m "docs: point README at vision/architecture and update platform prerequisites"
```

---

## Self-Review

**Spec coverage:**
- Platform upgrade (spec §5) → Tasks 1–5. Jetty EE11 (§5.2.1) → Task 2. Spring AI 2.0 drift (§5.2.2/§5.2.3) → Task 3. Boot 3.x deprecations (§5.2.6) → Task 4. Jackson boundary (§5.2.4) → documented in Task 7 §14. Upgrade acceptance (§5.3) → Task 5 Step 2.
- VISION (§6.1) → Task 6. ARCHITECTURE 14 sections (§6.2) → Task 7. Performance/NFRs (§7) → Task 7 §12/§13. ADR-007…012 (§8) → Task 8. Roadmap/status matrix (§3/§4) → Task 6. README (§10) → Task 9. Version bump (§10) → Task 5.

**Placeholder scan:** No TBD/TODO. The only intentionally deferred item — the exact Jetty patch — is now pinned to `12.1.11`.

**Type consistency:** EE10→EE11 package names and Jetty artifact IDs match the verified Maven coordinates; `spring-boot-dependencies`/`spring-ai-bom` artifactIds match existing parent POM; module names match the 10 real modules.

## Execution Handoff

See the message accompanying this plan for the execution-approach choice.
