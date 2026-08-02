# Lumina

**Pure-Java, server-driven UI** for interactive and AI-native apps.
Inspired by Streamlit — application code needs **no HTML, CSS, or JavaScript**.

!!! tip "Start here"
    New to Lumina? Read the **[Developer guide](DEVELOPER_GUIDE.md)** — standalone Java,
    Spring Boot, Spring AI, Jakarta EE, and other frameworks.

## Quick install (Maven Central)

```xml
<!-- Standalone / any JVM framework -->
<dependency>
  <groupId>io.github.wosslabs</groupId>
  <artifactId>lumina-web</artifactId>
  <version>1.0.0</version>
</dependency>
```

```xml
<!-- Spring Boot -->
<dependency>
  <groupId>io.github.wosslabs</groupId>
  <artifactId>lumina-spring-boot-starter</artifactId>
  <version>1.0.0</version>
</dependency>
```

Optional: `lumina-spring-ai`, `lumina-cli`.

## 30-second standalone app

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

Open [http://127.0.0.1:8080/](http://127.0.0.1:8080/).

## Documentation map

| Page | Audience |
|------|----------|
| [Developer guide](DEVELOPER_GUIDE.md) | Juniors & integrators (primary) |
| [Author cheat sheet](GUIDE.md) | Quick `Ui` API reference |
| [Product overview](PRODUCT.md) | What Lumina ships in 1.0 |
| [Architecture](ARCHITECTURE.md) | Module boundaries & design |
| [Extensions](EXTENSIONS.md) | Plugins, themes, AI SPI |
| [Migration](MIGRATION.md) | 1.0 API notes |
| [Releasing](RELEASING.md) | Maintainers / Maven Central |
| [ADRs](adr/ADR-001-module-boundaries.md) | Decision history |

## Project links

- Source: [github.com/wosslabs/lumina](https://github.com/wosslabs/lumina)
- License: [Apache-2.0](https://github.com/wosslabs/lumina/blob/main/LICENSE)
- Security: [SECURITY.md](https://github.com/wosslabs/lumina/blob/main/SECURITY.md)
