# Lumina Developer Guide

**Audience:** Java developers who want to add interactive, server-driven UI to an app —
especially juniors on Spring Boot / Spring AI, and teams on plain Java, Jakarta EE, or other
frameworks.

**Version:** Lumina `1.0.0`  
**Maven groupId:** `io.github.wosslabs`  
**Java packages:** `io.lumina.*`  
**License:** Apache-2.0

This guide is the main onboarding document. For a short API cheat sheet see [GUIDE.md](GUIDE.md).
For architecture and ADRs see [ARCHITECTURE.md](ARCHITECTURE.md).

---

## Table of contents

1. [What Lumina is](#1-what-lumina-is)
2. [Mental model (read this first)](#2-mental-model-read-this-first)
3. [Requirements](#3-requirements)
4. [Maven coordinates](#4-maven-coordinates)
5. [Choose an integration path](#5-choose-an-integration-path)
6. [Path A — Standalone (any JVM)](#6-path-a--standalone-any-jvm)
7. [Path B — Spring Boot](#7-path-b--spring-boot)
8. [Path C — Spring Boot + Spring AI](#8-path-c--spring-boot--spring-ai)
9. [Jakarta EE and other frameworks](#9-jakarta-ee-and-other-frameworks)
10. [CLI launcher](#10-cli-launcher)
11. [Building UIs](#11-building-uis)
12. [Session state](#12-session-state)
13. [Routing and the enterprise shell](#13-routing-and-the-enterprise-shell)
14. [AI surfaces](#14-ai-surfaces)
15. [Widgets reference](#15-widgets-reference)
16. [Configuration reference](#16-configuration-reference)
17. [Production checklist](#17-production-checklist)
18. [Troubleshooting](#18-troubleshooting)
19. [Examples in this repository](#19-examples-in-this-repository)
20. [Where to go next](#20-where-to-go-next)
21. [Hosting docs on Read the Docs](#21-hosting-docs-on-read-the-docs)

---

## 1. What Lumina is

Lumina is an open-source **Java** framework for interactive web UIs, inspired by Streamlit.

You write ordinary Java that declares widgets. Lumina:

- Owns the browser client (Web Components + CSS)
- Owns the WebSocket protocol
- Owns session state and reruns
- Patches the UI when the user interacts

**You do not write HTML, CSS, or JavaScript** for application UI.

Typical use cases:

- Internal tools and admin consoles
- AI chat / agent demos on the JVM
- Data and ops dashboards for Java teams
- Spring Boot apps that want a Streamlit-like front end without a separate SPA

---

## 2. Mental model (read this first)

### The `build()` loop

Your app implements one method:

```java
public interface LuminaApp {
    void build(Ui ui);
}
```

Every user interaction (button click, input change, chat submit, nav click, …) causes Lumina to
**rerun** `build(Ui)` for that browser session. On each run you declare the UI from top to bottom.
Lumina diffs the new component tree against the previous one and sends a small patch to the client.

This is the same idea as Streamlit’s script rerun.

### Consequences juniors must learn early

| Rule | Why it matters |
|------|----------------|
| `build()` must be **idempotent** given the same session state | It may run many times |
| Persist durable data in `ui.state()`, not local fields that reset every run | Fields on your app class are shared / confusing; session state is per browser session |
| Return values from widgets (`button`, `textInput`, …) reflect **this run** | `if (ui.button("Save")) { … }` runs the body only on the click rerun |
| Call `ui.pageConfig(...)` **before** other `Ui` methods when you use it | Required by the API contract |
| Do not block forever without streaming/UX feedback | Long work should use `spinner` or stream tokens with `ui.ai(TokenStream)` |

### What Lumina is not

- Not a general-purpose Jakarta Servlet replacement for your whole site
- Not a React/Vue framework (no author-owned frontend bundle)
- Not a multi-node clustered session store in 1.0 (single-node sessions)

---

## 3. Requirements

| Piece | Version |
|-------|---------|
| JDK | **25+** |
| Maven | **3.9+** (Gradle users: consume the same Maven coordinates) |
| Spring Boot (optional path) | **4.1.x** |
| Spring AI (optional path) | **2.0.x** |
| Embedded server | Jetty **12.1.x** (brought in by `lumina-web`) |

---

## 4. Maven coordinates

Artifacts are published to **Maven Central** under:

```text
groupId:    io.github.wosslabs
version:    1.0.0
```

> **Note:** After a Central publish, [search.maven.org](https://search.maven.org/) and the CDN
> can lag for a short time. The [Central Portal](https://central.sonatype.com/) artifact page is
> authoritative. If resolve fails, wait or clear your local cache for that artifact.

### Which artifact should I depend on?

| Artifact | Use when |
|----------|----------|
| `lumina-web` | **Standalone / any framework** — includes embedded Jetty server + client |
| `lumina-spring-boot-starter` | **Spring Boot** — auto-starts Lumina when a `LuminaApp` bean exists |
| `lumina-spring-ai` | Optional bridge from Spring AI `ChatClient` → Lumina streaming |
| `lumina-cli` | Optional `java -jar` style `run` launcher |
| `lumina-core` | Rarely direct — contracts only (`Ui`, `LuminaApp`, AI SPIs) |

`lumina-examples` is **not** published to Central (demo-only module in the GitHub repo).

---

## 5. Choose an integration path

```text
                    ┌─────────────────────────────┐
                    │ Do you use Spring Boot?     │
                    └─────────────┬───────────────┘
                          no │           │ yes
                             ▼           ▼
                    Path A: standalone   Path B: Boot starter
                    lumina-web           + @Bean LuminaApp
                                         │
                                         ▼
                               Need LLM via Spring AI?
                                         │ yes
                                         ▼
                               Path C: + lumina-spring-ai
                                       + Spring AI provider starter
```

| Path | Dependency | Bootstrap |
|------|------------|-----------|
| **A — Standalone** | `lumina-web` | `LuminaServer.start(app)` |
| **B — Spring Boot** | `lumina-spring-boot-starter` | `@Bean LuminaApp` (server auto-starts) |
| **C — Boot + AI** | starter + `lumina-spring-ai` + Spring AI | Inject Spring AI / wrap with `SpringAiChatClient` |
| **Jakarta EE / Quarkus / Micronaut / plain main** | `lumina-web` | Same as Path A from a startup hook |

All paths share the same `LuminaApp` + `Ui` programming model.

---

## 6. Path A — Standalone (any JVM)

Best for: plain Java apps, Jakarta EE sidecars, Quarkus/Micronaut companions, CLIs, demos.

### 6.1 Dependency

```xml
<dependency>
  <groupId>io.github.wosslabs</groupId>
  <artifactId>lumina-web</artifactId>
  <version>1.0.0</version>
</dependency>
```

### 6.2 Minimal app

```java
package com.example;

import io.lumina.LuminaApp;
import io.lumina.ui.Ui;
import io.lumina.web.LuminaServer;
import io.lumina.web.LuminaServerConfig;

public final class HelloLumina implements LuminaApp {
    @Override
    public void build(Ui ui) {
        ui.title("Hello Lumina");
        ui.text("No HTML. No CSS. No JavaScript.");
        if (ui.button("Click me")) {
            ui.markdown("**You clicked!** This text appears on the rerun.");
        }
    }

    public static void main(String[] args) {
        LuminaServer server = LuminaServer.start(
                new HelloLumina(),
                LuminaServerConfig.builder()
                        .host("127.0.0.1")  // loopback only (safe default)
                        .port(8080)
                        .build());
        System.out.println("Open " + server.uri());
    }
}
```

Open [http://127.0.0.1:8080/](http://127.0.0.1:8080/).

### 6.3 Server configuration tips

```java
LuminaServerConfig.builder()
    .host("127.0.0.1")          // default: loopback
    .port(8080)                 // 0 = ephemeral (good for tests)
    .maxSessions(100)           // concurrent WebSocket sessions
    .idleTimeout(Duration.ofMinutes(30))
    .allowedOrigins(Set.of())   // empty = same-host/localhost default
    .build();
```

To expose beyond this machine (behind a reverse proxy with TLS):

```java
.host("0.0.0.0")
.allowedOrigins(Set.of("https://apps.example.com"))
```

### 6.4 Offline AI without Spring

```java
import io.lumina.ai.ChatClients;

var chat = ChatClients.echo();
String reply = ui.ai(chat.stream(prompt)); // streams tokens to the browser
```

Or:

```java
import io.lumina.ai.EchoAiProvider;

var provider = new EchoAiProvider();
ui.ai(provider.stream(prompt));
```

---

## 7. Path B — Spring Boot

Best for: teams already on Spring Boot who want Lumina as an embedded UI process.

### 7.1 Dependency

```xml
<dependency>
  <groupId>io.github.wosslabs</groupId>
  <artifactId>lumina-spring-boot-starter</artifactId>
  <version>1.0.0</version>
</dependency>
```

The starter pulls `lumina-web` transitively.

### 7.2 Application

```java
package com.example;

import io.lumina.LuminaApp;
import io.lumina.ui.PageConfig;
import io.lumina.ui.Ui;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    LuminaApp luminaApp() {
        return ui -> {
            ui.pageConfig(PageConfig.builder().title("Demo").build());
            ui.title("Spring Boot + Lumina");
            String name = ui.textInput("Name");
            if (ui.button("Greet") && !name.isBlank()) {
                ui.markdown("Hello, **" + name.trim() + "**");
            }
        };
    }
}
```

### 7.3 Properties

```yaml
# application.yml
lumina:
  port: 8080
```

When a `LuminaApp` bean is present, auto-configuration starts the embedded Lumina server on that
port (default `8080`). Port `0` requests an ephemeral port.

### 7.4 Inject collaborators

Because `LuminaApp` is a Spring bean, you can inject services:

```java
@Bean
LuminaApp luminaApp(OrderService orders) {
    return ui -> {
        ui.title("Orders");
        if (ui.button("Refresh")) {
            ui.table(orders.asRows());
        } else {
            ui.table(orders.asRows());
        }
    };
}
```

Prefer reading from services **inside** `build()`, and write durable UI/session data to
`ui.state()`.

### 7.5 Coexistence with Spring MVC / WebFlux

Lumina starts its **own** embedded Jetty server (not your Boot servlet container). That means:

- Boot may listen on `8080` for REST **and** Lumina may also want `8080` → **port conflict**
- Solution: put Lumina on another port, e.g. `lumina.port=8090`, and open that URL for the UI

```yaml
server:
  port: 8080          # Spring MVC / Actuator
lumina:
  port: 8090          # Lumina UI
```

---

## 8. Path C — Spring Boot + Spring AI

Best for: AI chat / agent UIs backed by OpenAI, Ollama, or other Spring AI models.

### 8.1 Dependencies (example: OpenAI)

```xml
<dependency>
  <groupId>io.github.wosslabs</groupId>
  <artifactId>lumina-spring-boot-starter</artifactId>
  <version>1.0.0</version>
</dependency>
<dependency>
  <groupId>io.github.wosslabs</groupId>
  <artifactId>lumina-spring-ai</artifactId>
  <version>1.0.0</version>
</dependency>

<!-- Pick a Spring AI provider starter that matches your Spring AI BOM -->
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-starter-model-openai</artifactId>
</dependency>
```

Import the Spring AI BOM in `dependencyManagement` (version **2.0.0** aligns with Lumina 1.0).

### 8.2 Configuration (never hardcode keys)

```yaml
lumina:
  port: 8090
  ai:
    provider: openai   # documentation hint; wire the real ChatModel via Spring AI

spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4o-mini
```

```bash
export OPENAI_API_KEY=sk-...
```

For local models with Ollama, use the Ollama Spring AI starter and point Spring AI at your local
endpoint instead.

### 8.3 Streaming chat app

`lumina-spring-ai` adapts Spring AI’s fluent `ChatClient` to Lumina’s `TokenStream`:

```java
package com.example;

import io.lumina.LuminaApp;
import io.lumina.springai.SpringAiChatClient;
import io.lumina.ui.Ui;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AiDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiDemoApplication.class, args);
    }

    @Bean
    ChatClient springAiChatClient(ChatModel chatModel) {
        return ChatClient.create(chatModel);
    }

    @Bean
    LuminaApp luminaApp(ChatClient springAiChatClient) {
        var luminaChat = new SpringAiChatClient(springAiChatClient);
        return ui -> buildChat(ui, luminaChat);
    }

    private static void buildChat(Ui ui, SpringAiChatClient chat) {
        ui.title("AI Chat");
        List<String[]> history = ui.state().computeIfAbsent("history", k -> new ArrayList<>());

        for (String[] turn : history) {
            ui.user(turn[0]);
            ui.ai(turn[1]);
        }

        String prompt = ui.chatInput();
        if (prompt != null) {
            ui.user(prompt);
            // Streams tokens to the browser; returns the full reply when finished
            String reply = ui.ai(chat.stream(prompt));
            history.add(new String[] {prompt, reply});
        }
    }
}
```

### 8.4 Auto-configuration note

If a Spring AI `ChatModel` bean is present, `lumina-spring-ai` can auto-register a Lumina
`io.lumina.ai.ChatClient` bean (`SpringAiChatClient`). You can inject that type directly:

```java
@Bean
LuminaApp app(io.lumina.ai.ChatClient chat) {
    return ui -> {
        String prompt = ui.chatInput();
        if (prompt != null) {
            ui.ai(chat.stream(prompt));
        }
    };
}
```

### 8.5 Develop without an API key

Use the built-in echo client until credentials are ready:

```java
import io.lumina.ai.ChatClients;

var chat = ChatClients.echo();
ui.ai(chat.stream(prompt));
```

### 8.6 Rich AI widgets

```java
ui.citation("Architecture", "docs/ARCHITECTURE.md", "Server-driven component trees.");
ui.ragSources(List.of(
    Map.of("title", "Guide", "url", "docs/GUIDE.md", "score", 0.92)
));
ui.toolCall("search", "ok", Map.of("q", prompt), Map.of("hits", 3));
ui.usage(120, 48, 0.001, 80L);
```

Agent-oriented widgets: `agentTimeline`, `toolInvocation`, `approval`, `memoryPanel`
(see the `AgentDemoApp` example in the GitHub repo).

---

## 9. Jakarta EE and other frameworks

Lumina **embeds Jetty** and speaks its own WebSocket protocol. It is not shipped as a
`jakarta.servlet.HttpServlet` WAR drop-in for 1.0. The supported pattern on Jakarta EE / other
stacks is: **start `LuminaServer` from a lifecycle hook** (same programming model as Path A).

### 9.1 Jakarta EE (CDI) sketch

```java
import io.lumina.web.LuminaServer;
import io.lumina.web.LuminaServerConfig;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class LuminaBootstrap {
    private LuminaServer server;

    @PostConstruct
    void start() {
        server = LuminaServer.start(
                new MyLuminaApp(),
                LuminaServerConfig.builder().port(8090).build());
    }

    @PreDestroy
    void stop() {
        if (server != null) {
            server.stop();
        }
    }
}
```

Use a **dedicated port** so you do not collide with the app server’s HTTP listener.

### 9.2 Quarkus / Micronaut / Dropwizard

Same idea: on application start → `LuminaServer.start(app, config)`; on shutdown → `server.stop()`.
Keep Lumina UI code in plain Java (`LuminaApp`) so it stays framework-agnostic.

### 9.3 “Can I mount Lumina under my existing host/path?”

1.0 serves its own HTTP + WebSocket endpoint from the embedded server. Path-based mounting behind
a reverse proxy is an ops concern (proxy `/` or `/lumina/` to Lumina’s port). There is no first-class
“attach to external ServletContext” API yet.

---

## 10. CLI launcher

```xml
<dependency>
  <groupId>io.github.wosslabs</groupId>
  <artifactId>lumina-cli</artifactId>
  <version>1.0.0</version>
</dependency>
```

```bash
java -cp "your-app.jar:lib/*" io.lumina.cli.LuminaCli run com.example.HelloLumina
```

`HelloLumina` must implement `LuminaApp` and expose a public no-arg constructor (or a static
factory the CLI can reflectively invoke — see `LuminaCli` javadoc).

---

## 11. Building UIs

### 11.1 Page config (first call)

```java
ui.pageConfig(PageConfig.builder()
        .title("My App")
        .layout(PageLayout.WIDE)           // or CENTERED
        .sidebar(SidebarState.EXPANDED)    // or COLLAPSED
        .build());
```

### 11.2 Content basics

```java
ui.title("Dashboard");                 // H1 for the view
ui.header(h -> h.title("Context"));  // banner context — not an H1
ui.markdown("Use **Markdown** for rich text.");
ui.text("Plain text.");
ui.code("java", "System.out.println(\"hi\");");
ui.json(Map.of("ok", true));
ui.image("/assets/logo.png");
ui.progress(0.42);
```

### 11.3 Layout

```java
ui.columns(2, cols -> {
    cols[0].markdown("Left");
    cols[1].markdown("Right");
});

ui.container(box -> {
    box.markdown("#### Card-like section");
    box.text("Grouped content");
});

ui.expander("Details", body -> body.text("Hidden until expanded"));

ui.tabs(List.of("One", "Two"), panels -> {
    panels[0].text("Tab one body");
    panels[1].text("Tab two body");
});

ui.dialog("Confirm", body -> body.text("Are you sure?"));
ui.notify("Saved");
ui.themeToggle();
```

### 11.4 Forms and actions

```java
String name = ui.textInput("Name");
boolean agree = ui.checkbox("I agree", false);
double qty = ui.numberInput("Qty", 1.0, 1.0, 100.0, 1.0);
String region = ui.selectbox("Region", List.of("US", "EU", "APAC"));
String plan = ui.radio("Plan", List.of("Free", "Pro"), 0);
double volume = ui.slider("Volume", 0, 100, 50, 5);

if (ui.button("Submit")) {
    ui.state().set("lastName", name);
    ui.notify("Submitted");
}
```

### 11.5 Files and downloads

```java
ui.fileUpload("Upload CSV").ifPresent(file -> {
    ui.text("Got " + file.fileName() + " (" + file.bytes().length + " bytes)");
});

byte[] report = "a,b\n1,2\n".getBytes(StandardCharsets.UTF_8);
if (ui.downloadButton("Download", report, "report.csv")) {
    ui.text("Download started");
}
```

Downloads are capped at **1 MiB** in 1.0.

### 11.6 Long work

```java
ui.spinner("Loading report", () -> {
    // blocking work on the session thread
    ui.text("Done");
});
```

---

## 12. Session state

```java
var state = ui.state();

int count = state.computeIfAbsent("count", k -> 0);
if (ui.button("Inc")) {
    state.set("count", count + 1);
}
ui.markdown("Count: **" + state.get("count") + "**");
```

| Method | Purpose |
|--------|---------|
| `get(key)` | Read (or `null`) |
| `set(key, value)` | Write |
| `computeIfAbsent(key, fn)` | Init once per session |
| `contains(key)` | Presence |
| `remove(key)` | Delete |

**Tips**

- Keys are strings; values should be simple serializable-ish objects (primitives, lists, maps).
- Keys starting with `__lumina.` are reserved for framework concerns (path, theme, roles, …).
- State is **per browser session**, not global across users.

---

## 13. Routing and the enterprise shell

### 13.1 Path API

```java
String path = ui.path();      // e.g. "/" or "/about"
ui.navigate("/about");        // set path for this / future runs
```

The client sends `location.pathname` on connect and syncs via `history.replaceState`.

### 13.2 Sidebar brand / nav / footer

```java
ui.sidebar(sidebar -> {
    sidebar.brand(b -> {
        b.markdown("## Acme");
        b.text("Internal tools");
    });
    sidebar.nav(nav -> {
        nav.page("Home", "/");
        nav.page("Chat", "/chat");
        nav.page("About", "/about");
    });
    sidebar.footer(f -> f.markdown("_v1.0.0_"));
});

switch (ui.path()) {
    case "/chat" -> buildChat(ui);
    case "/about" -> buildAbout(ui);
    default -> buildHome(ui);
}
```

Follow [UX_CONSTITUTION.md](UX_CONSTITUTION.md) for accessibility and chrome expectations.

---

## 14. AI surfaces

### Contracts

| Type | Role |
|------|------|
| `io.lumina.ai.ChatClient` | `prompt` + `stream` |
| `io.lumina.ai.TokenStream` | Blocking iterable of text chunks |
| `io.lumina.ai.AiProvider` | `stream(prompt)` only |
| `ChatClients.echo()` / `EchoAiProvider` | Offline stub |
| `SpringAiChatClient` | Spring AI adapter (implements both ChatClient + AiProvider) |

### Blocking vs streaming

```java
// Blocking full reply (no live token UI)
String reply = chat.prompt(prompt);
ui.ai(reply);

// Streaming (preferred for LLMs)
String reply = ui.ai(chat.stream(prompt));
```

### Chat transcript pattern

Always replay history from `ui.state()` at the start of `build()`, then append on new input
(see §8.3). Do not rely on “widgets from last run still being there” — each run rebuilds the tree.

---

## 15. Widgets reference

| API | Returns | Notes |
|-----|---------|-------|
| `title` / `text` / `markdown` | void | Content |
| `button` | `boolean` | `true` on click rerun |
| `textInput` / `chatInput` | `String` / nullable | Chat submit is nullable until send |
| `checkbox` | `boolean` | |
| `numberInput` / `slider` | `double` | Overloads for bounds/step |
| `selectbox` / `radio` | `String` | |
| `spinner` | void | Wraps blocking body |
| `downloadButton` | `boolean` | ≤ 1 MiB |
| `fileUpload` | `Optional<UploadedFile>` | |
| `table` | void | `List<Map<String,Object>>` rows |
| `progress` | void | 0.0–1.0 |
| `code` / `json` / `image` | void | |
| `user` / `ai` | void / `String` | Chat bubbles; streaming overload returns full text |
| `citation` / `ragSources` / `toolCall` / `usage` | void | AI metadata |
| `agentTimeline` / `toolInvocation` / `approval` / `memoryPanel` | varies | Agents |
| `columns` / `container` / `expander` / `tabs` / `dialog` | void | Layout / chrome |
| `notify` / `themeToggle` | void | UX |
| `rolesAllowed` / `t` | void / `String` | Enterprise hooks |
| `sidebar` / `header` / `navigate` / `path` / `pageConfig` | — | Shell |

---

## 16. Configuration reference

### Lumina (Spring Boot)

| Property | Default | Meaning |
|----------|---------|---------|
| `lumina.port` | `8080` | Embedded Lumina HTTP/WebSocket port |
| `lumina.ai.provider` | `echo` | Hint for docs/tooling; wire real models via Spring AI |

### `LuminaServerConfig` (standalone)

| Builder method | Default | Meaning |
|----------------|---------|---------|
| `host` | `127.0.0.1` | Bind address |
| `port` | `8080` | Bind port (`0` = ephemeral) |
| `maxSessions` | `100` | Concurrent WS sessions |
| `idleTimeout` | 30 minutes | Idle WS close |
| `allowedOrigins` | empty (same-host default) | Explicit Origin allowlist |

---

## 17. Production checklist

- [ ] Bind loopback or private network; put TLS termination on a reverse proxy
- [ ] Set explicit `allowedOrigins` when exposing beyond localhost
- [ ] Separate Spring MVC port from `lumina.port` if both run in one process
- [ ] Keep API keys in env / secret manager (`OPENAI_API_KEY`, etc.)
- [ ] Do not log prompt/PII payloads; audit hooks log intent names only by design
- [ ] Cap upload/download expectations (1 MiB download limit)
- [ ] Load-test session count vs `maxSessions`
- [ ] Remember: **no multi-node session clustering** in 1.0 — sticky sessions / single instance

---

## 18. Troubleshooting

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| Blank page | Port already in use / old process | `lsof -i :8080` and kill, or change port |
| `BindException` | Another server on the port | Change `lumina.port` / `LuminaServerConfig.port` |
| Maven cannot resolve `io.github.wosslabs` | CDN index lag after publish | Wait, or verify on Central Portal; `mvn -U` |
| UI does not update as expected | Mutating local fields instead of `ui.state()` | Use session state |
| Button body never seems to run | Logic not gated on `if (ui.button(...))` | Gate side effects on the boolean |
| Spring AI empty/failing | Missing API key / wrong starter | Check env vars and Spring AI config |
| CORS / WS rejected | Origin not allowed | Configure `allowedOrigins` |
| `pageConfig` errors | Called after other widgets | Call it first |

---

## 19. Examples in this repository

Clone [https://github.com/wosslabs/lumina](https://github.com/wosslabs/lumina):

```bash
mvn -q -pl lumina-examples -am install
mvn -q -pl lumina-examples exec:java -Dexec.mainClass=io.lumina.examples.showcase.ShowcaseMain
# http://127.0.0.1:8080/
```

| Example | Main class | Shows |
|---------|------------|-------|
| Showcase | `…showcase.ShowcaseMain` | Shell, routing, widgets, AI cards |
| Hello AI | `…helloai.HelloAiMain` | Stateful chat (echo) |
| Streaming chat | `…streaming.StreamingChatMain` | `ui.ai(TokenStream)` |
| Agent demo | `…agent.AgentDemoApp` | Timeline / approvals / memory |
| Layout demo | `…layout.LayoutDemoMain` | Columns / containers / expanders |

---

## 20. Where to go next

| Doc | Purpose |
|-----|---------|
| [PRODUCT.md](PRODUCT.md) | Product overview |
| [GUIDE.md](GUIDE.md) | Short author cheat sheet |
| [ARCHITECTURE.md](ARCHITECTURE.md) | System design |
| [EXTENSIONS.md](EXTENSIONS.md) | Plugins / themes / AI SPI |
| [MIGRATION.md](MIGRATION.md) | 1.0 API notes |
| [UX_CONSTITUTION.md](UX_CONSTITUTION.md) | A11y / UX PR checklist |
| [RELEASING.md](RELEASING.md) | Maintainers: Central publish |
| [CHANGELOG.md](../CHANGELOG.md) | Release notes |

---

## 21. Hosting docs on Read the Docs

This repository is configured for [Read the Docs](https://readthedocs.org/):

| File | Role |
|------|------|
| [`.readthedocs.yaml`](https://github.com/wosslabs/lumina/blob/main/.readthedocs.yaml) | RTD build (Python + MkDocs) |
| [`mkdocs.yml`](https://github.com/wosslabs/lumina/blob/main/mkdocs.yml) | Site nav, Material theme |
| [`docs/requirements.txt`](requirements.txt) | Pinned MkDocs deps |
| [`docs/index.md`](index.md) | Docs home page |

Published site (once the RTD project is linked and the first build succeeds):
**https://wosslabs-lumina.readthedocs.io/**

### Local preview

```bash
python3 -m venv .venv-docs
source .venv-docs/bin/activate
pip install -r docs/requirements.txt
mkdocs serve
# open http://127.0.0.1:8000/
```

### RTD dashboard checklist

1. Import the GitHub repo `wosslabs/lumina` (already connected to your account).
2. Confirm the default branch is `main`.
3. Ensure **Build** uses the repository `.readthedocs.yaml` (MkDocs, not Sphinx).
4. Trigger a build; fix any errors shown in the build log.
5. Optional: set a custom domain under **Domains**.

---

### Quick start card (copy/paste)

**Standalone**

```xml
<dependency>
  <groupId>io.github.wosslabs</groupId>
  <artifactId>lumina-web</artifactId>
  <version>1.0.0</version>
</dependency>
```

```java
LuminaServer.start(ui -> ui.title("Hello"));
```

**Spring Boot**

```xml
<dependency>
  <groupId>io.github.wosslabs</groupId>
  <artifactId>lumina-spring-boot-starter</artifactId>
  <version>1.0.0</version>
</dependency>
```

```java
@Bean LuminaApp app() { return ui -> ui.title("Hello"); }
```

Open `http://127.0.0.1:<port>/` and start declaring widgets.
