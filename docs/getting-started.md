# Getting started

Lumina is a **Java** framework for interactive, server-driven web apps.
You write `LuminaApp.build(Ui ui)` — Lumina owns HTML/CSS/JS, sessions, and live patches.

## Choose a path

| Your stack | Start with | Artifact |
|------------|------------|----------|
| Plain Java / Jakarta EE / Quarkus / Micronaut | [Developer guide § Path A](DEVELOPER_GUIDE.md#6-path-a--standalone-any-jvm) | `lumina-web` |
| Spring Boot | [Developer guide § Path B](DEVELOPER_GUIDE.md#7-path-b--spring-boot) | `lumina-spring-boot-starter` |
| Spring Boot + Spring AI | [Developer guide § Path C](DEVELOPER_GUIDE.md#8-path-c--spring-boot--spring-ai) | starter + `lumina-spring-ai` |
| RAG / agents / MCP UIs | [AI guide](AI_GUIDE.md) | same as above + examples |

!!! warning "Boot + Spring Web"
    If your POM includes `spring-boot-starter-web` / `webmvc`, set `lumina.port=8090` and open
    **http://127.0.0.1:8090/**. `http://localhost:8080/` often shows **Disconnected** — that is
    Tomcat without Lumina’s WebSocket. Details: [Developer guide §7](DEVELOPER_GUIDE.md#7-path-b--spring-boot).

## Requirements

- **Java 25+**
- **Maven 3.9+**
- Optional: Spring Boot **4.1**, Spring AI **2.0**

## Hello world (standalone)

```xml
<dependency>
  <groupId>io.github.wosslabs</groupId>
  <artifactId>lumina-web</artifactId>
  <version>1.0.0</version>
</dependency>
```

```java
import io.lumina.web.LuminaServer;

public class Main {
  public static void main(String[] args) {
    LuminaServer.start(ui -> {
      ui.title("Hello Lumina");
      if (ui.button("Click me")) {
        ui.markdown("**It works.**");
      }
    });
  }
}
```

Open [http://127.0.0.1:8080/](http://127.0.0.1:8080/).

## Next steps

1. Read the full **[Developer guide](DEVELOPER_GUIDE.md)** (mental model, routing, widgets, production).
2. Run the **showcase** (includes RAG / Agent / MCP pages) — see [AI guide](AI_GUIDE.md).
3. Skim **[Architecture](ARCHITECTURE.md)** when you need module boundaries.
