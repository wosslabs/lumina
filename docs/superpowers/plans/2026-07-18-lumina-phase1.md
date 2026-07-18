# Lumina Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship a working Lumina MVP that runs Hello AI over WebSocket with the full Phase 1 component set, Spring-free core, and optional Spring Boot starter.

**Architecture:** Multi-module Clean Architecture — `lumina-core` owns public APIs; `session`/`components`/`runtime` implement the server-side tree + serial rerun loop; `lumina-web` embeds Jetty for HTTP/WebSocket and serves a thin Web Components client; starter/CLI/devtools sit at the edges.

**Tech Stack:** Java 21 (release flag), Maven 3.9+, JUnit 5, AssertJ, Jetty 12 (WebSocket), Jackson 2.18, Spring Boot 3.4.x (starter + examples only).

## Global Constraints

- GroupId: `io.lumina`; artifacts: `lumina-*`; version: `0.1.0-SNAPSHOT`
- Packages: public `io.lumina`, `io.lumina.ui`, `io.lumina.state`, `io.lumina.ai`, `io.lumina.spi` only
- `lumina-core` and `lumina-runtime` must not depend on Spring, Jetty, or Servlet APIs
- App contract: `LuminaApp#build(Ui ui)`; transport: WebSocket JSON; state: hybrid keys + `StateStore`
- AI: `ChatClient` SPI + `ChatClients.echo()`; no Spring AI in Phase 1
- Spec: `docs/superpowers/specs/2026-07-18-lumina-phase1-design.md`
- Verify with `mvn -q clean test` after logic changes; never claim done without green tests
- Commits: small, cohesive; only when the human asks (or when executing a plan task that explicitly includes a commit step and the human approved plan execution)

---

## File Structure

```
lumina-parent/pom.xml
docs/adr/ADR-001-module-boundaries.md
docs/adr/ADR-002-rerun-concurrency.md
docs/adr/ADR-003-wire-protocol-diff.md
docs/adr/ADR-004-state-keying.md
docs/adr/ADR-005-embedded-server.md

lumina-core/pom.xml
lumina-core/src/main/java/io/lumina/LuminaApp.java
lumina-core/src/main/java/io/lumina/LuminaException.java
lumina-core/src/main/java/io/lumina/ui/Ui.java
lumina-core/src/main/java/io/lumina/ui/UploadedFile.java
lumina-core/src/main/java/io/lumina/state/StateStore.java
lumina-core/src/main/java/io/lumina/ai/ChatClient.java
lumina-core/src/main/java/io/lumina/ai/ChatClients.java
lumina-core/src/main/java/io/lumina/ai/EchoChatClient.java
lumina-core/src/main/java/io/lumina/spi/Transport.java
lumina-core/src/main/java/io/lumina/model/ComponentNode.java
lumina-core/src/main/java/io/lumina/model/ComponentTypes.java
lumina-core/src/test/java/io/lumina/ai/EchoChatClientTest.java

lumina-session/pom.xml
lumina-session/src/main/java/io/lumina/session/internal/MapStateStore.java
lumina-session/src/main/java/io/lumina/session/internal/WidgetState.java
lumina-session/src/main/java/io/lumina/session/internal/SessionState.java
lumina-session/src/test/java/io/lumina/session/internal/MapStateStoreTest.java
lumina-session/src/test/java/io/lumina/session/internal/WidgetStateTest.java

lumina-components/pom.xml
lumina-components/src/main/java/io/lumina/components/ComponentSpecs.java

lumina-runtime/pom.xml
lumina-runtime/src/main/java/io/lumina/runtime/UiBinder.java
lumina-runtime/src/main/java/io/lumina/runtime/RunResult.java
lumina-runtime/src/main/java/io/lumina/runtime/Intent.java
lumina-runtime/src/main/java/io/lumina/runtime/SessionManager.java
lumina-runtime/src/main/java/io/lumina/runtime/SessionHandle.java
lumina-runtime/src/main/java/io/lumina/runtime/SessionExecutor.java
lumina-runtime/src/main/java/io/lumina/runtime/AppRunner.java
lumina-runtime/src/main/java/io/lumina/diff/PatchOp.java
lumina-runtime/src/main/java/io/lumina/diff/TreeDiffer.java
lumina-runtime/src/main/java/io/lumina/Lumina.java
lumina-runtime/src/test/java/io/lumina/runtime/UiBinderTest.java
lumina-runtime/src/test/java/io/lumina/diff/TreeDifferTest.java
lumina-runtime/src/test/java/io/lumina/runtime/AppRunnerTest.java

lumina-web/pom.xml
lumina-web/src/main/java/io/lumina/web/LuminaServer.java
lumina-web/src/main/java/io/lumina/web/LuminaServerConfig.java
lumina-web/src/main/java/io/lumina/web/internal/JettyLuminaHttpServer.java
lumina-web/src/main/java/io/lumina/web/internal/LuminaWebSocketEndpoint.java
lumina-web/src/main/java/io/lumina/web/internal/ProtocolCodec.java
lumina-web/src/main/java/io/lumina/web/internal/LuminaHttpServer.java
lumina-web/src/main/resources/lumina-web/index.html
lumina-web/src/main/resources/lumina-web/lumina-client.js
lumina-web/src/main/resources/lumina-web/lumina.css
lumina-web/src/test/java/io/lumina/web/ProtocolCodecTest.java
lumina-web/src/test/java/io/lumina/web/LuminaServerIT.java

lumina-devtools/pom.xml
lumina-devtools/src/main/java/io/lumina/devtools/ReloadSpi.java
lumina-devtools/src/main/java/io/lumina/devtools/NoOpReloader.java

lumina-spring-boot-starter/pom.xml
lumina-spring-boot-starter/src/main/java/io/lumina/spring/LuminaAutoConfiguration.java
lumina-spring-boot-starter/src/main/java/io/lumina/spring/LuminaProperties.java
lumina-spring-boot-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
lumina-spring-boot-starter/src/test/java/io/lumina/spring/LuminaAutoConfigurationTest.java

lumina-cli/pom.xml
lumina-cli/src/main/java/io/lumina/cli/LuminaCli.java

lumina-examples/pom.xml
lumina-examples/src/main/java/io/lumina/examples/helloai/HelloAiApp.java
lumina-examples/src/main/java/io/lumina/examples/helloai/HelloAiMain.java
```

---

### Task 1: Parent POM, reactor modules, ADR-001

**Files:**
- Create: `pom.xml` (root parent / reactor)
- Create: `lumina-core/pom.xml`, `lumina-session/pom.xml`, `lumina-components/pom.xml`, `lumina-runtime/pom.xml`, `lumina-web/pom.xml`, `lumina-devtools/pom.xml`, `lumina-spring-boot-starter/pom.xml`, `lumina-cli/pom.xml`, `lumina-examples/pom.xml`
- Create: `docs/adr/ADR-001-module-boundaries.md`
- Create: `.gitignore`

**Interfaces:**
- Consumes: nothing
- Produces: Maven reactor that compiles empty jars; dependency edges match spec §5

- [ ] **Step 1: Write ADR-001**

Create `docs/adr/ADR-001-module-boundaries.md`:

```markdown
# ADR-001: Module & package boundaries

## Status
Accepted

## Context
Lumina must remain maintainable for a decade with optional Spring Boot and a Spring-free core.

## Decision
Use multi-module Maven with hard dependency rules from the Phase 1 design spec.
Public API packages: `io.lumina`, `io.lumina.ui`, `io.lumina.state`, `io.lumina.ai`, `io.lumina.spi`.
`lumina-core` and `lumina-runtime` must not depend on Spring, Jetty, or Servlet APIs.
Jetty is confined to `lumina-web`.

## Consequences
Slightly more scaffolding; clear binary-compat and SPI surfaces.
```

- [ ] **Step 2: Create root parent POM**

Create root `pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>io.lumina</groupId>
  <artifactId>lumina-parent</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <packaging>pom</packaging>
  <name>Lumina Parent</name>
  <modules>
    <module>lumina-core</module>
    <module>lumina-session</module>
    <module>lumina-components</module>
    <module>lumina-runtime</module>
    <module>lumina-web</module>
    <module>lumina-devtools</module>
    <module>lumina-spring-boot-starter</module>
    <module>lumina-cli</module>
    <module>lumina-examples</module>
  </modules>
  <properties>
    <java.version>21</java.version>
    <maven.compiler.release>21</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <junit.version>5.11.4</junit.version>
    <assertj.version>3.27.3</assertj.version>
    <jetty.version>12.0.16</jetty.version>
    <jackson.version>2.18.3</jackson.version>
    <spring-boot.version>3.4.4</spring-boot.version>
  </properties>
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>io.lumina</groupId>
        <artifactId>lumina-core</artifactId>
        <version>${project.version}</version>
      </dependency>
      <dependency>
        <groupId>io.lumina</groupId>
        <artifactId>lumina-session</artifactId>
        <version>${project.version}</version>
      </dependency>
      <dependency>
        <groupId>io.lumina</groupId>
        <artifactId>lumina-components</artifactId>
        <version>${project.version}</version>
      </dependency>
      <dependency>
        <groupId>io.lumina</groupId>
        <artifactId>lumina-runtime</artifactId>
        <version>${project.version}</version>
      </dependency>
      <dependency>
        <groupId>io.lumina</groupId>
        <artifactId>lumina-web</artifactId>
        <version>${project.version}</version>
      </dependency>
      <dependency>
        <groupId>org.junit</groupId>
        <artifactId>junit-bom</artifactId>
        <version>${junit.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
      <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <version>${assertj.version}</version>
      </dependency>
      <dependency>
        <groupId>org.eclipse.jetty</groupId>
        <artifactId>jetty-bom</artifactId>
        <version>${jetty.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
      <dependency>
        <groupId>com.fasterxml.jackson</groupId>
        <artifactId>jackson-bom</artifactId>
        <version>${jackson.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
      <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-dependencies</artifactId>
        <version>${spring-boot.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
  <build>
    <pluginManagement>
      <plugins>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-compiler-plugin</artifactId>
          <version>3.13.0</version>
          <configuration>
            <release>${maven.compiler.release}</release>
          </configuration>
        </plugin>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-surefire-plugin</artifactId>
          <version>3.5.2</version>
        </plugin>
      </plugins>
    </pluginManagement>
  </build>
</project>
```

- [ ] **Step 3: Create module POMs**

For each module, create a minimal `pom.xml` with parent `lumina-parent` and correct dependencies:

| Module | packaging | dependencies |
|--------|-----------|--------------|
| `lumina-core` | jar | junit/assertj test |
| `lumina-session` | jar | `lumina-core`; junit/assertj test |
| `lumina-components` | jar | `lumina-core` |
| `lumina-runtime` | jar | `core`, `session`, `components`; junit/assertj test |
| `lumina-web` | jar | `runtime`, jetty-server, jetty-ee10-servlet, jetty-ee10-websocket-jetty-server, jackson-databind; junit/assertj test |
| `lumina-devtools` | jar | `runtime` |
| `lumina-spring-boot-starter` | jar | `web`, `spring-boot-starter`; `spring-boot-starter-test` test |
| `lumina-cli` | jar | `web` |
| `lumina-examples` | jar | `lumina-spring-boot-starter` (or `web` for plain main) |

Each module needs a placeholder so the jar is non-empty later; for now add `package-info.java` under the module's root package, e.g. `lumina-core/src/main/java/io/lumina/package-info.java`:

```java
/**
 * Lumina public API root.
 */
package io.lumina;
```

Create `.gitignore`:

```
target/
.idea/
*.iml
.classpath
.project
.settings/
.DS_Store
```

- [ ] **Step 4: Verify reactor builds**

Run: `mvn -q clean install -DskipTests`

Expected: `BUILD SUCCESS` for all modules.

- [ ] **Step 5: Commit**

```bash
git add pom.xml .gitignore docs/adr/ADR-001-module-boundaries.md lumina-*/pom.xml lumina-*/src
git commit -m "$(cat <<'EOF'
build: scaffold Lumina multi-module Maven reactor

Establish module boundaries and ADR-001 before runtime work.
EOF
)"
```

---

### Task 2: Core model, exceptions, ChatClient

**Files:**
- Create: files listed under `lumina-core` for model/AI/exception
- Test: `lumina-core/src/test/java/io/lumina/ai/EchoChatClientTest.java`

**Interfaces:**
- Consumes: reactor from Task 1
- Produces:
  - `LuminaApp#build(Ui)`
  - `ComponentNode(String id, String type, Map<String, Object> props, List<ComponentNode> children)`
  - `ChatClient#prompt(String): String`
  - `ChatClients.echo(): ChatClient`
  - `LuminaException`

- [ ] **Step 1: Write failing test**

```java
package io.lumina.ai;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EchoChatClientTest {
    @Test
    void promptEchoesWithPrefix() {
        ChatClient client = ChatClients.echo();
        assertThat(client.prompt("hello")).isEqualTo("Echo: hello");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl lumina-core test -Dtest=EchoChatClientTest`

Expected: FAIL (classes missing).

- [ ] **Step 3: Implement core types**

`LuminaApp.java`:

```java
package io.lumina;

import io.lumina.ui.Ui;

/**
 * Entry point for a Lumina application. Invoked on each session rerun.
 */
@FunctionalInterface
public interface LuminaApp {
    /**
     * Declaratively build the UI for the current run.
     *
     * @param ui UI binder for this run; must not be retained across runs
     */
    void build(Ui ui);
}
```

`LuminaException.java`:

```java
package io.lumina;

/**
 * Base unchecked exception for Lumina framework errors.
 */
public class LuminaException extends RuntimeException {
    public LuminaException(String message) {
        super(message);
    }

    public LuminaException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

`ComponentNode.java` (immutable record):

```java
package io.lumina.model;

import java.util.List;
import java.util.Map;

/**
 * Immutable node in the server-side component tree.
 *
 * @param id stable node id
 * @param type component type constant
 * @param props serializable properties
 * @param children ordered children
 */
public record ComponentNode(
        String id,
        String type,
        Map<String, Object> props,
        List<ComponentNode> children
) {
    public ComponentNode {
        props = Map.copyOf(props);
        children = List.copyOf(children);
    }
}
```

`ComponentTypes.java`:

```java
package io.lumina.model;

/** Built-in component type constants (wire + binder). */
public final class ComponentTypes {
    public static final String ROOT = "root";
    public static final String TITLE = "title";
    public static final String MARKDOWN = "markdown";
    public static final String TEXT = "text";
    public static final String BUTTON = "button";
    public static final String TEXT_INPUT = "text_input";
    public static final String CHAT_INPUT = "chat_input";
    public static final String USER_MESSAGE = "user_message";
    public static final String AI_MESSAGE = "ai_message";
    public static final String CODE = "code";
    public static final String JSON = "json";
    public static final String TABLE = "table";
    public static final String IMAGE = "image";
    public static final String FILE_UPLOAD = "file_upload";
    public static final String PROGRESS = "progress";

    private ComponentTypes() {}
}
```

`ChatClient.java`, `EchoChatClient.java`, `ChatClients.java`:

```java
package io.lumina.ai;

/**
 * SPI for language-model style completions.
 */
public interface ChatClient {
    /**
     * Produce a completion for the given prompt.
     *
     * @param input user prompt; never null
     * @return model reply; never null
     */
    String prompt(String input);
}
```

```java
package io.lumina.ai;

final class EchoChatClient implements ChatClient {
    @Override
    public String prompt(String input) {
        return "Echo: " + input;
    }
}
```

```java
package io.lumina.ai;

/** Factory for built-in {@link ChatClient} implementations. */
public final class ChatClients {
    private ChatClients() {}

    /** Offline stub that prefixes replies with {@code Echo: }. */
    public static ChatClient echo() {
        return new EchoChatClient();
    }
}
```

Stub `Ui.java` temporarily if needed for `LuminaApp` compile — prefer completing `Ui` in Task 3 in the same commit wave if the compiler requires it. Minimal stub:

```java
package io.lumina.ui;

/** Declarative UI binder for a single {@code LuminaApp#build} run. */
public interface Ui {
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl lumina-core test -Dtest=EchoChatClientTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add lumina-core
git commit -m "$(cat <<'EOF'
feat(core): add ComponentNode, LuminaApp, and ChatClient SPI

Introduce the public app contract and echo ChatClient for Hello AI.
EOF
)"
```

---

### Task 3: Public Ui + StateStore + UploadedFile APIs

**Files:**
- Create/Modify: `lumina-core/src/main/java/io/lumina/ui/Ui.java`
- Create: `lumina-core/src/main/java/io/lumina/ui/UploadedFile.java`
- Create: `lumina-core/src/main/java/io/lumina/state/StateStore.java`
- Create: `lumina-core/src/main/java/io/lumina/spi/Transport.java`
- Test: `lumina-core/src/test/java/io/lumina/state/StateStoreContractDocTest.java` (compile-only / javadoc sanity) — prefer real tests in session module; here add a compile test that `Ui` method signatures match the spec via a fake implementing class in test sources

**Interfaces:**
- Consumes: Task 2 types
- Produces: full `Ui` and `StateStore` method signatures from the design spec

- [ ] **Step 1: Write failing compile test (fake Ui)**

```java
package io.lumina.ui;

import io.lumina.state.StateStore;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class UiSignatureTest {
    static final class FakeUi implements Ui {
        @Override public void title(String text) {}
        @Override public void markdown(String md) {}
        @Override public void text(String text) {}
        @Override public boolean button(String label) { return false; }
        @Override public String textInput(String label) { return ""; }
        @Override public String chatInput() { return null; }
        @Override public void user(String message) {}
        @Override public void ai(String message) {}
        @Override public void code(String language, String source) {}
        @Override public void json(Object value) {}
        @Override public void table(List<Map<String, Object>> rows) {}
        @Override public void image(String urlOrResource) {}
        @Override public Optional<UploadedFile> fileUpload(String label) { return Optional.empty(); }
        @Override public void progress(double value) {}
        @Override public StateStore state() { return null; }
        @Override public <T> T withKey(String key, Function<Ui, T> block) { return block.apply(this); }
    }

    @Test
    void fakeUiCompilesAgainstPublicContract() {
        Ui ui = new FakeUi();
        ui.title("x");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl lumina-core test -Dtest=UiSignatureTest`

Expected: FAIL until `Ui` methods exist.

- [ ] **Step 3: Implement public APIs with Javadoc**

Replace stub `Ui` with the full interface from the design spec (all methods + Javadoc on each). Implement:

```java
package io.lumina.state;

import java.util.function.Function;

/**
 * Session-scoped key/value store for app-owned state.
 */
public interface StateStore {
    <T> T get(String key);
    void set(String key, Object value);
    <T> T computeIfAbsent(String key, Function<String, T> mappingFunction);
    boolean contains(String key);
    void remove(String key);
}
```

```java
package io.lumina.ui;

/**
 * Uploaded file available during the run that received it.
 *
 * @param fileName original file name
 * @param contentType MIME type if known
 * @param bytes file bytes
 */
public record UploadedFile(String fileName, String contentType, byte[] bytes) {
    public UploadedFile {
        bytes = bytes == null ? new byte[0] : bytes.clone();
    }
}
```

```java
package io.lumina.spi;

/**
 * Transport SPI placeholder. Phase 1 ships WebSocket only in lumina-web.
 */
public interface Transport {
    String name();
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl lumina-core test`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add lumina-core
git commit -m "$(cat <<'EOF'
feat(core): define Ui, StateStore, and UploadedFile public APIs

Lock Phase 1 DSL signatures for binders and session state.
EOF
)"
```

---

### Task 4: Session state implementation + ADR-004

**Files:**
- Create: `docs/adr/ADR-004-state-keying.md`
- Create: `lumina-session/src/main/java/io/lumina/session/internal/MapStateStore.java`
- Create: `lumina-session/src/main/java/io/lumina/session/internal/WidgetState.java`
- Create: `lumina-session/src/main/java/io/lumina/session/internal/SessionState.java`
- Test: `MapStateStoreTest.java`, `WidgetStateTest.java`

**Interfaces:**
- Consumes: `StateStore`
- Produces:
  - `MapStateStore` implements `StateStore`
  - `WidgetState#value(String key)`, `#set(String key, Object value)`, `#consumeClick(String key): boolean`, `#consumeChatSubmit(String key): String`
  - `SessionState` aggregates `StateStore` + `WidgetState`

- [ ] **Step 1: Write ADR-004 and failing tests**

ADR-004 decision: auto-key = `path + "/" + type + "#" + index`; explicit `withKey` pushes path segment; widget values persist across runs; button click and chat submit are single-consume per run.

```java
package io.lumina.session.internal;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MapStateStoreTest {
    @Test
    void computeIfAbsentReturnsSameInstance() {
        MapStateStore store = new MapStateStore();
        var list = store.computeIfAbsent("history", k -> new java.util.ArrayList<String>());
        list.add("a");
        assertThat(store.<java.util.List<String>>get("history")).containsExactly("a");
    }
}
```

```java
class WidgetStateTest {
    @Test
    void clickIsConsumedOncePerRun() {
        WidgetState widgets = new WidgetState();
        widgets.set("btn", Boolean.TRUE);
        assertThat(widgets.consumeClick("btn")).isTrue();
        assertThat(widgets.consumeClick("btn")).isFalse();
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -q -pl lumina-session test`

Expected: FAIL.

- [ ] **Step 3: Implement session internals**

`MapStateStore`: thread-confined `HashMap` (session queue guarantees single-thread access).

`WidgetState`: map of key → value; `consumeClick` returns true once then clears; `consumeChatSubmit` returns String once then clears; text input values persist.

`SessionState`:

```java
public final class SessionState {
    private final MapStateStore store = new MapStateStore();
    private final WidgetState widgets = new WidgetState();
    public StateStore store() { return store; }
    public WidgetState widgets() { return widgets; }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -q -pl lumina-session test`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add docs/adr/ADR-004-state-keying.md lumina-session
git commit -m "$(cat <<'EOF'
feat(session): implement StateStore and widget key state

Add hybrid session state with single-consume click/chat intents.
EOF
)"
```

---

### Task 5: UiBinder (tree building) for vertical-slice components

**Files:**
- Create: `lumina-runtime/src/main/java/io/lumina/runtime/UiBinder.java`
- Create: `lumina-runtime/src/main/java/io/lumina/runtime/RunResult.java`
- Create: `lumina-components/src/main/java/io/lumina/components/ComponentSpecs.java` (prop key constants)
- Test: `lumina-runtime/src/test/java/io/lumina/runtime/UiBinderTest.java`
- Create: `docs/adr/ADR-002-rerun-concurrency.md` (document serial queue; implement queue in Task 7)

**Interfaces:**
- Consumes: `Ui`, `SessionState`, `ComponentNode`, `ComponentTypes`
- Produces: `UiBinder(SessionState)` implements `Ui`; `RunResult(ComponentNode root)` after `LuminaApp#build`

Vertical slice components in this task: `title`, `text`, `chatInput`, `user`, `ai`, `state`, `withKey`. Remaining DSL methods throw `UnsupportedOperationException` until Task 10 — **or** implement all binders here with correct tree nodes (preferred if small).

**Preferred:** implement all `Ui` methods in `UiBinder` in this task so Task 10 is client-only.

- [ ] **Step 1: Write failing test**

```java
package io.lumina.runtime;

import io.lumina.model.ComponentTypes;
import io.lumina.session.internal.SessionState;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class UiBinderTest {
    @Test
    void buildEmitsTitleAndChatNodes() {
        SessionState session = new SessionState();
        UiBinder ui = new UiBinder(session);
        ui.title("Hello AI");
        ui.chatInput();
        var root = ui.buildRoot();
        assertThat(root.children()).extracting(n -> n.type())
                .contains(ComponentTypes.TITLE, ComponentTypes.CHAT_INPUT);
    }

    @Test
    void chatInputReturnsSubmittedPromptOnce() {
        SessionState session = new SessionState();
        session.widgets().setChatSubmit("auto:/chat_input#0", "hi");
        UiBinder ui = new UiBinder(session);
        assertThat(ui.chatInput()).isEqualTo("hi");
        assertThat(ui.chatInput()).isNull(); // second call different key index
    }
}
```

Adjust auto-key strings to match ADR-004 implementation exactly.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl lumina-runtime -am test -Dtest=UiBinderTest`

Expected: FAIL.

- [ ] **Step 3: Implement UiBinder**

Responsibilities:
- Maintain child list for current path
- Allocate ids/keys via counter stack
- Read/write `WidgetState` for inputs/buttons/uploads
- `buildRoot()` returns `ComponentNode` type `root`
- `json(Object)` stores Jackson-serializable structure as prop `value` (runtime may depend on Jackson **or** store `String.valueOf` / Map only — prefer **no Jackson in runtime**: require `value` already JSON-friendly (`Map`, `List`, `String`, numbers); for arbitrary objects call `String.valueOf` in Phase 1)

Implement all MVP methods.

Write ADR-002 stating: one serial queue per session; virtual threads; no concurrent build.

- [ ] **Step 4: Run tests**

Run: `mvn -q -pl lumina-runtime -am test -Dtest=UiBinderTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add docs/adr/ADR-002-rerun-concurrency.md lumina-runtime lumina-components
git commit -m "$(cat <<'EOF'
feat(runtime): implement UiBinder component tree construction

Bind Ui DSL calls to immutable ComponentNode trees per session run.
EOF
)"
```

---

### Task 6: TreeDiffer + ADR-003

**Files:**
- Create: `docs/adr/ADR-003-wire-protocol-diff.md`
- Create: `lumina-runtime/src/main/java/io/lumina/diff/PatchOp.java`
- Create: `lumina-runtime/src/main/java/io/lumina/diff/TreeDiffer.java`
- Test: `lumina-runtime/src/test/java/io/lumina/diff/TreeDifferTest.java`

**Interfaces:**
- Consumes: `ComponentNode`
- Produces: `List<PatchOp>` where `PatchOp` is a record:
  - `String op` — `ADD` | `REMOVE` | `REPLACE` | `UPDATE_PROPS` | `REORDER`
  - `String path` — JSON-pointer-like path e.g. `/children/0`
  - `ComponentNode node` — nullable except ADD/REPLACE
  - `Map<String, Object> props` — nullable except UPDATE_PROPS
  - `List<String> order` — nullable except REORDER (child ids)

- [ ] **Step 1: Write failing tests**

```java
@Test
void appendChildProducesSingleAdd() {
    ComponentNode before = root(title("a"));
    ComponentNode after = root(title("a"), text("b"));
    List<PatchOp> ops = new TreeDiffer().diff(before, after);
    assertThat(ops).hasSize(1);
    assertThat(ops.getFirst().op()).isEqualTo("ADD");
}

@Test
void identicalTreesProduceNoOps() {
    ComponentNode tree = root(title("a"));
    assertThat(new TreeDiffer().diff(tree, tree)).isEmpty();
}
```

Helper methods in test to build nodes.

- [ ] **Step 2: Run to verify fail**

Run: `mvn -q -pl lumina-runtime test -Dtest=TreeDifferTest`

Expected: FAIL.

- [ ] **Step 3: Implement keyed sibling diff**

Algorithm:
1. If types/ids differ at node → `REPLACE`
2. If same id/type and props differ → `UPDATE_PROPS`
3. Diff children by id: removed → `REMOVE`, added → `ADD`, common → recurse
4. If child id order changed → `REORDER`

Document wire shapes in ADR-003 (intent/snapshot/patch/error).

- [ ] **Step 4: Run tests**

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add docs/adr/ADR-003-wire-protocol-diff.md lumina-runtime/src/main/java/io/lumina/diff lumina-runtime/src/test/java/io/lumina/diff
git commit -m "$(cat <<'EOF'
feat(runtime): add keyed TreeDiffer and patch op model

Enable incremental UI updates for the WebSocket protocol.
EOF
)"
```

---

### Task 7: AppRunner + SessionManager (headless rerun loop)

**Files:**
- Create: `Intent.java`, `SessionHandle.java`, `SessionExecutor.java`, `SessionManager.java`, `AppRunner.java`, `Lumina.java`
- Test: `AppRunnerTest.java`

**Interfaces:**
- Consumes: `LuminaApp`, `UiBinder`, `TreeDiffer`, `SessionState`
- Produces:
  - `AppRunner.run(LuminaApp, SessionState, Intent): RunResult` where `RunResult` has `root()` and `patches()`
  - `SessionManager.create(LuminaApp): SessionHandle`
  - `SessionHandle.submit(Intent): CompletableFuture<RunResult>`
  - `Lumina.bootstrap(LuminaApp)` deferred to web module — here only `Lumina.newSessionManager()` helper in runtime:

```java
public final class Lumina {
    private Lumina() {}
    public static SessionManager sessionManager(LuminaApp app) {
        return new SessionManager(app);
    }
}
```

`Intent` record:

```java
public record Intent(String name, String targetId, Map<String, Object> payload) {
    public static Intent connect() { return new Intent("connect", null, Map.of()); }
    public static Intent chatSubmit(String targetId, String text) {
        return new Intent("submit_chat", targetId, Map.of("value", text));
    }
    public static Intent click(String targetId) {
        return new Intent("click", targetId, Map.of());
    }
    public static Intent input(String targetId, String value) {
        return new Intent("input", targetId, Map.of("value", value));
    }
}
```

- [ ] **Step 1: Write failing test**

```java
@Test
void connectThenChatSubmitProducesAiEchoInTree() {
    LuminaApp app = ui -> {
        ui.title("Hello AI");
        var history = ui.state().computeIfAbsent("history", k -> new ArrayList<String[]>());
        for (String[] turn : history) {
            ui.user(turn[0]);
            ui.ai(turn[1]);
        }
        String prompt = ui.chatInput();
        if (prompt != null) {
            String reply = ChatClients.echo().prompt(prompt);
            history.add(new String[]{prompt, reply});
            ui.user(prompt);
            ui.ai(reply);
        }
    };
    SessionManager mgr = new SessionManager(app);
    SessionHandle session = mgr.create();
    RunResult initial = session.submit(Intent.connect()).join();
    String chatId = findType(initial.root(), ComponentTypes.CHAT_INPUT).id();
    RunResult after = session.submit(Intent.chatSubmit(chatId, "ping")).join();
    assertThat(flattenTypes(after.root())).contains(ComponentTypes.USER_MESSAGE, ComponentTypes.AI_MESSAGE);
}
```

- [ ] **Step 2: Run to verify fail**

Run: `mvn -q -pl lumina-runtime -am test -Dtest=AppRunnerTest`

Expected: FAIL.

- [ ] **Step 3: Implement serial session executor**

`SessionExecutor`: per-session `BlockingQueue` + single virtual thread loop; submit returns `CompletableFuture`.

`AppRunner`:
1. Apply intent to `WidgetState` (map targetId → key)
2. `UiBinder` + `app.build(ui)`
3. Diff against previous root
4. Store new root; return patches (empty on first connect — caller sends snapshot)

On first connect: patches empty; `RunResult.fullSnapshot(true)`.

Catch user exceptions → wrap `LuminaException`; keep previous tree; surface error message on `RunResult`.

- [ ] **Step 4: Run tests**

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add lumina-runtime
git commit -m "$(cat <<'EOF'
feat(runtime): add session-scoped AppRunner and serial executor

Drive connect/intent/rerun/diff without a browser.
EOF
)"
```

---

### Task 8: Web server, protocol codec, ADR-005

**Files:**
- Create: ADR-005, `LuminaHttpServer`, `JettyLuminaHttpServer`, `ProtocolCodec`, `LuminaWebSocketEndpoint`, `LuminaServer`, `LuminaServerConfig`
- Test: `ProtocolCodecTest.java`, `LuminaServerIT.java`

**Interfaces:**
- Consumes: `SessionManager`, `RunResult`, Jackson
- Produces:
  - `LuminaServer.start()` / `stop()` / `port()`
  - `Lumina.bootstrap(LuminaApp app)` in `io.lumina` (runtime or web — **place bootstrap in `lumina-web`** as `io.lumina.web.LuminaBootstrap` and re-export convenience `io.lumina.Lumina.bootstrap` only if runtime can take an optional server SPI; simplest Phase 1: `io.lumina.web.LuminaServer.start(app)`)

Public bootstrap API:

```java
package io.lumina.web;

public final class LuminaServer {
    public static LuminaServer start(LuminaApp app) { ... }
    public static LuminaServer start(LuminaApp app, LuminaServerConfig config) { ... }
    public int port();
    public void stop();
    public URI uri();
}
```

Move or add `Lumina.bootstrap` documentation pointing apps to `LuminaServer.start` — update design note: bootstrap lives in `lumina-web` for Phase 1 to keep runtime Jetty-free. Spec's `Lumina.bootstrap` becomes:

```java
// lumina-web
public final class Lumina {
    public static LuminaServer bootstrap(LuminaApp app) {
        return LuminaServer.start(app);
    }
}
```

Package: `io.lumina.web.Lumina` **or** keep `io.lumina.Lumina` inside `lumina-web` module (allowed: same package split across modules is discouraged). **Decision for implementers:** use `io.lumina.web.LuminaServer` as the public bootstrap; document in README.

- [ ] **Step 1: Write ADR-005 + failing codec test**

ADR-005: Jetty 12 embedded; abstraction `LuminaHttpServer`.

```java
@Test
void encodesSnapshot() {
    String json = ProtocolCodec.toSnapshotJson(sampleRoot());
    assertThat(json).contains("\"type\":\"snapshot\"");
}
```

- [ ] **Step 2: Run to verify fail**

Run: `mvn -q -pl lumina-web -am test -Dtest=ProtocolCodecTest`

Expected: FAIL.

- [ ] **Step 3: Implement Jetty server + WS endpoint**

Behavior:
- `GET /` → `index.html`
- Static `/lumina-web/**`
- WS `/ws`
- On open: `sessionManager.create()`, run `Intent.connect()`, send snapshot
- On message: parse intent, `submit`, send `patch` or `error`
- Bind port `0` in tests; default `8080` in config

- [ ] **Step 4: Integration test with JDK HttpClient WebSocket**

```java
@Test
void websocketReceivesSnapshot() throws Exception {
    LuminaServer server = LuminaServer.start(ui -> ui.title("T"), LuminaServerConfig.builder().port(0).build());
    try {
        // connect WS, read first text message, assert type snapshot
    } finally {
        server.stop();
    }
}
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add docs/adr/ADR-005-embedded-server.md lumina-web
git commit -m "$(cat <<'EOF'
feat(web): embed Jetty WebSocket server and JSON protocol

Serve snapshots/patches for session connect and intents.
EOF
)"
```

---

### Task 9: Browser client (Web Components) — vertical slice

**Files:**
- Create: `index.html`, `lumina-client.js`, `lumina.css`

**Interfaces:**
- Consumes: snapshot/patch protocol
- Produces: working UI for `title`, `text`, `chat_input`, `user_message`, `ai_message`

- [ ] **Step 1: Manual acceptance checklist (write as test doc in IT comments)**

Client must:
1. Open `ws://host/ws`
2. On `snapshot`, render root children
3. On `patch`, apply ADD/REMOVE/REPLACE/UPDATE_PROPS/REORDER
4. Chat form submit → `{type:"intent", name:"submit_chat", targetId, payload:{value}}`

- [ ] **Step 2: Implement minimal CSS + HTML shell**

`index.html` loads `/lumina-web/lumina.css` and `/lumina-web/lumina-client.js`; body contains `<lumina-app></lumina-app>`.

- [ ] **Step 3: Implement `lumina-client.js`**

Custom elements:
- `lumina-app` — root host
- `lumina-title`, `lumina-text`, `lumina-chat-input`, `lumina-user-message`, `lumina-ai-message`

Keep JS small and dependency-free.

- [ ] **Step 4: Manual verify**

Run: `mvn -q -pl lumina-examples -am package` (after Task 11) or a temporary `main` in web tests.

For this task, add `lumina-web/src/test/java/io/lumina/web/HelloSliceMain.java` (not a test) **or** use `LuminaServerIT` + fetch `index.html` asserting 200.

Run: `mvn -q -pl lumina-web -am test -Dtest=LuminaServerIT`

Expected: PASS + `GET /` returns HTML containing `lumina-app`.

- [ ] **Step 5: Commit**

```bash
git add lumina-web/src/main/resources/lumina-web
git commit -m "$(cat <<'EOF'
feat(web): add Web Components client for chat vertical slice

Render title/text/chat over WebSocket snapshots and patches.
EOF
)"
```

---

### Task 10: Remaining MVP components (server props + client elements)

**Files:**
- Modify: `UiBinder.java` (if any stubs remain)
- Modify: `lumina-client.js`, `lumina.css`
- Test: extend `UiBinderTest` for each component type’s node shape

**Interfaces:**
- Consumes: Task 5 binder
- Produces: nodes + WC for: markdown, button, text_input, code, json, table, image, file_upload, progress

Prop contracts (lock these):

| type | props |
|------|-------|
| `markdown` | `content: String` |
| `button` | `label: String` |
| `text_input` | `label: String`, `value: String` |
| `code` | `language: String`, `source: String` |
| `json` | `value: Object` |
| `table` | `rows: List<Map<String,Object>>` |
| `image` | `src: String` |
| `file_upload` | `label: String` |
| `progress` | `value: Number` |

- [ ] **Step 1: Write failing binder tests for table/button/progress**

```java
@Test
void tableNodeContainsRows() {
    UiBinder ui = new UiBinder(new SessionState());
    ui.table(List.of(Map.of("a", 1)));
    assertThat(ui.buildRoot().children().getFirst().props().get("rows")).isNotNull();
}
```

- [ ] **Step 2: Run to verify fail if missing**

- [ ] **Step 3: Implement client custom elements + styles for each type**

Markdown: safe subset — escape HTML, support `#` headings and newlines as `<br>` in Phase 1 (no full CommonMark dependency required). Document limitation in Javadoc on `Ui#markdown`.

File upload: `<input type="file">` → intent `file_upload` with base64 payload (size limit 1MB in server codec).

- [ ] **Step 4: Run unit tests**

Run: `mvn -q -pl lumina-runtime,lumina-web -am test`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add lumina-runtime lumina-web
git commit -m "$(cat <<'EOF'
feat: complete Phase 1 MVP component set

Add binder props and Web Components for all remaining widgets.
EOF
)"
```

---

### Task 11: Hello AI example module

**Files:**
- Create: `HelloAiApp.java`, `HelloAiMain.java`
- Modify: `lumina-examples/pom.xml` to depend on `lumina-web`

**Interfaces:**
- Consumes: `LuminaApp`, `Ui`, `ChatClients`, `LuminaServer`
- Produces: runnable main under 20 lines of app logic in `HelloAiApp`

- [ ] **Step 1: Implement HelloAiApp exactly from the design spec**

User logic in `HelloAiApp#build` must remain &lt; 20 lines.

- [ ] **Step 2: Implement main**

```java
public final class HelloAiMain {
    public static void main(String[] args) {
        var server = LuminaServer.start(new HelloAiApp());
        System.out.println("Lumina Hello AI at " + server.uri());
    }
}
```

- [ ] **Step 3: Add exec plugin or document**

```bash
mvn -q -pl lumina-examples -am package exec:java -Dexec.mainClass=io.lumina.examples.helloai.HelloAiMain
```

- [ ] **Step 4: Smoke test (optional automated)**

Extend `LuminaServerIT` to use `HelloAiApp`, submit chat, assert patch/snapshot contains `Echo:`.

- [ ] **Step 5: Commit**

```bash
git add lumina-examples
git commit -m "$(cat <<'EOF'
feat(examples): add Hello AI sample application

Demonstrate chat + StateStore history in under 20 lines of app code.
EOF
)"
```

---

### Task 12: Spring Boot starter

**Files:**
- Create: `LuminaProperties`, `LuminaAutoConfiguration`, AutoConfiguration.imports
- Test: `LuminaAutoConfigurationTest.java`

**Interfaces:**
- Consumes: `LuminaServer`, `LuminaApp` bean
- Produces: server starts when app bean present; property `lumina.port` default 8080

- [ ] **Step 1: Write failing Spring context test**

```java
@SpringBootTest(classes = {LuminaAutoConfiguration.class, TestApp.class})
class LuminaAutoConfigurationTest {
    @Test
    void startsServer(@Autowired LuminaServer server) {
        assertThat(server.port()).isPositive();
    }
    @TestConfiguration
    static class TestApp {
        @Bean LuminaApp app() { return ui -> ui.title("boot"); }
    }
}
```

Configure random port via `lumina.port=0`.

- [ ] **Step 2: Run to verify fail**

- [ ] **Step 3: Implement auto-configuration**

`@AutoConfiguration`, `@EnableConfigurationProperties`, `@ConditionalOnBean(LuminaApp.class)`, `@ConditionalOnMissingBean(LuminaServer.class)`, start on `@PostConstruct` / `SmartLifecycle`, stop on shutdown.

- [ ] **Step 4: Run test**

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add lumina-spring-boot-starter
git commit -m "$(cat <<'EOF'
feat(starter): auto-configure LuminaServer from LuminaApp bean

Provide optional Spring Boot integration without contaminating core.
EOF
)"
```

---

### Task 13: CLI + devtools skeletons

**Files:**
- Create: `ReloadSpi.java`, `NoOpReloader.java`
- Create: `LuminaCli.java`
- Test: `LuminaCliTest` parsing `--help`

**Interfaces:**
- Consumes: `LuminaServer`
- Produces:
  - `ReloadSpi` with `void onChange(Runnable rebuild)`
  - CLI: `lumina run --class fqcn` using current classpath reflection

- [ ] **Step 1: Write failing CLI help test**

```java
@Test
void helpExitsZero() {
    int code = LuminaCli.run(new String[]{"--help"}, new StringWriter());
    assertThat(code).isEqualTo(0);
}
```

- [ ] **Step 2: Implement skeleton CLI + NoOpReloader**

CLI commands Phase 1: `--help`, `run --class <fqcn>` (loads `LuminaApp` via no-arg ctor or static `create()`).

- [ ] **Step 3: Run tests**

- [ ] **Step 4: Commit**

```bash
git add lumina-cli lumina-devtools
git commit -m "$(cat <<'EOF'
feat: add CLI and devtools skeletons

Stub hot-reload SPI and lumina run entrypoint for later DX work.
EOF
)"
```

---

### Task 14: Root README + Javadoc pass + full verification

**Files:**
- Create: `README.md`
- Modify: any public API missing Javadoc

- [ ] **Step 1: Write README** covering mission, quickstart (`HelloAiMain`), module map, link to design spec

- [ ] **Step 2: Javadoc audit** — every public type/method in `io.lumina*` exported packages has Javadoc

- [ ] **Step 3: Full verify**

Run: `mvn -q clean test`

Expected: `BUILD SUCCESS`

Run: `mvn -q clean package`

Expected: `BUILD SUCCESS`

- [ ] **Step 4: Commit**

```bash
git add README.md lumina-*/src/main/java
git commit -m "$(cat <<'EOF'
docs: add README and complete public API Javadoc

Finish Phase 1 documentation gate for the MVP release.
EOF
)"
```

---

## Spec coverage checklist

| Spec item | Task |
|-----------|------|
| Multi-module reactor | 1 |
| ADR-001…005 | 1, 4, 5/7, 6, 8 |
| `LuminaApp` / `Ui` / packages | 2–3 |
| Hybrid state | 4–5 |
| Component tree + diff | 5–6 |
| Serial virtual-thread sessions | 7 |
| WebSocket protocol + Jetty | 8 |
| Web Components client | 9–10 |
| MVP components | 5 + 10 |
| ChatClient echo | 2 |
| Hello AI &lt;20 lines | 11 |
| Spring starter | 12 |
| CLI + devtools stubs | 13 |
| Tests from beginning | 2–13 |
| Javadoc public API | 2–3, 14 |
| C4 diagrams | already in design spec |

## Deferred (explicitly out of this plan)

- Token streaming, SSE, Spring AI, Redis sessions, full hot reload, rich markdown/CommonMark

---

## Self-review notes (author)

- Bootstrap clarified as `LuminaServer` in `lumina-web` to keep runtime Jetty-free (aligns with ADR-005 fix in spec).
- All MVP components scheduled; vertical slice reaches Hello AI at Task 11 after Tasks 7–9.
- No TBD steps remain; versions pinned in parent POM properties.
