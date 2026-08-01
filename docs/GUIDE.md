# Lumina Guide

## Widgets

Lumina renders widgets from Java and reruns `build()` when users interact. Values remain scoped to the current session.

```java
boolean enabled = ui.checkbox("Enable notifications", true);
double retries = ui.numberInput("Retries", 3.0, 0.0, 10.0, 1.0);
String region = ui.selectbox("Region", List.of("US East", "EU West"));
String plan = ui.radio("Plan", List.of("Free", "Team"), 1);
double volume = ui.slider("Volume", 0.0, 100.0, 50.0, 5.0);
```

`spinner` renders during a blocking operation and is removed when the operation completes:

```java
ui.spinner("Loading report", () -> ui.text("Report ready."));
```

`downloadButton` begins a browser download immediately and returns `true` for the rerun caused by its click. Downloads are limited to 1 MiB.

```java
if (ui.downloadButton("Download report", bytes, "report.csv")) {
    ui.text("Download started.");
}
```
# Lumina Author Guide

Framework-owned UI in pure Java. See also [PRODUCT.md](PRODUCT.md) and [VISION.md](VISION.md).

## App entry

```java
public class MyApp implements LuminaApp {
  @Override public void build(Ui ui) {
    ui.pageConfig(PageConfig.builder().title("My App").build());
    ui.sidebar(sb -> {
      sb.brand(b -> b.markdown("## My App"));
      sb.nav(nav -> nav.page("Home", "/"));
    });
    ui.title("Hello");
  }
}
```

## Routing

- `ui.path()` — current path  
- `ui.navigate("/x")` — set path this run  
- Client sends `connect` with `location.pathname`

## Shell

Prefer `sidebar.brand` / `sidebar.nav` / `sidebar.footer` and optional `ui.header`.

## Widgets

*(P2 fills this section.)*

## AI and agents

Use `AiProvider` for provider-neutral streaming. The built-in `EchoAiProvider` works without keys:

```java
AiProvider provider = new EchoAiProvider();
ui.ai(provider.stream("Summarize Lumina"));
ui.citation("Architecture", "docs/ARCHITECTURE.md", "Server-driven component trees.");
ui.usage(120, 48, 0.001, 80L);
```

`lumina-spring-ai` provides `SpringAiChatClientProvider` for an application-managed Spring AI
`ChatClient`. Configure `lumina.ai.provider=echo`, `openai`, or `ollama`; echo is the safe default.
Agent views use `agentTimeline`, `toolInvocation`, `approval`, and `memoryPanel`.

## Advanced UX

Use `tabs`, `dialog`, `notify`, and `themeToggle` for framework-owned interactions. Tabs provide
tablist/tab/tabpanel semantics, notifications use a polite live region, and the theme preference is
stored in session state under `__lumina.theme`.

## Enterprise hooks

Store authenticated role names under `__lumina.roles` to use `ui.rolesAllowed(roles, body)`.
`AuditLogger` receives only session and intent names; payloads are deliberately excluded. Store
translation maps under `__lumina.messages` and resolve them with `ui.t(key)`.

## UX standards

PRs touching UI must satisfy `docs/superpowers/specs/2026-08-01-lumina-ux-constitution-checklist.md`.
