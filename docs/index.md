# Lumina

<div class="lumina-hero" markdown>

# Lumina

**Pure-Java, server-driven UI** for interactive and AI-native apps.

Inspired by Streamlit — application code needs **no HTML, CSS, or JavaScript**.
Declare widgets in Java; Lumina owns the thin client, sessions, and live updates.

</div>

!!! tip "Where to start"
    - New to Lumina? Read the **[Developer guide](DEVELOPER_GUIDE.md)**.
    - Building RAG / agents / MCP UIs? Read the **[AI guide](AI_GUIDE.md)**.
    - Prefer a short API list? See the **[Author cheat sheet](GUIDE.md)**.

## Install from Maven Central

**Standalone / any JVM framework**

```xml
<dependency>
  <groupId>io.github.wosslabs</groupId>
  <artifactId>lumina-web</artifactId>
  <version>1.0.0</version>
</dependency>
```

**Spring Boot**

```xml
<dependency>
  <groupId>io.github.wosslabs</groupId>
  <artifactId>lumina-spring-boot-starter</artifactId>
  <version>1.0.0</version>
</dependency>
```

Optional artifacts: `lumina-spring-ai`, `lumina-cli`.

!!! note "Coordinates"
    Maven `groupId` is `io.github.wosslabs`. Java packages remain `io.lumina.*`.

## 30-second app

```java
import io.lumina.web.LuminaServer;

public class Main {
  public static void main(String[] args) {
    LuminaServer.start(ui -> {
      ui.title("Hello Lumina");
      ui.text("No HTML. No CSS. No JavaScript.");
    });
  }
}
```

Then open [http://127.0.0.1:8080/](http://127.0.0.1:8080/).

## Explore the docs

<div class="grid cards" markdown>

-   **[Developer guide](DEVELOPER_GUIDE.md)**

    ---

    Standalone, Spring Boot, Spring AI, Jakarta EE, and other JVM frameworks.

-   **[AI guide](AI_GUIDE.md)**

    ---

    RAG chat, agent approval loops, and MCP-style tool consoles.

-   **[Product overview](PRODUCT.md)**

    ---

    What ships in 1.0 and who Lumina is for.

-   **[Architecture](ARCHITECTURE.md)**

    ---

    Modules, rerun model, transport, and design boundaries.

-   **[Extensions](EXTENSIONS.md)**

    ---

    Plugins, themes, and AI provider SPIs.

-   **[UX constitution](UX_CONSTITUTION.md)**

    ---

    Accessibility and shell checklist for UI changes.

</div>

## Run the showcase

```bash
mvn -q -pl lumina-examples -am install
mvn -q -pl lumina-examples exec:java \
  -Dexec.mainClass=io.lumina.examples.showcase.ShowcaseMain
```

Sidebar demos include **RAG**, **Agent**, and **MCP tools** at `/rag`, `/agent`, and `/mcp`.

## Project links

| Resource | Link |
|----------|------|
| Source | [github.com/wosslabs/lumina](https://github.com/wosslabs/lumina) |
| Docs site | [wosslabs-lumina.readthedocs.io](https://wosslabs-lumina.readthedocs.io/) |
| License | [Apache-2.0](https://github.com/wosslabs/lumina/blob/main/LICENSE) |
| Security | [SECURITY.md](https://github.com/wosslabs/lumina/blob/main/SECURITY.md) |
