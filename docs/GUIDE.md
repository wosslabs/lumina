# Lumina Author Guide

Framework-owned UI in pure Java. See also [PRODUCT.md](PRODUCT.md) and [VISION.md](VISION.md).

## Requirements

- Java **25+**
- Maven **3.9+**

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

Start with the embedded server:

```java
LuminaServer.start(new MyApp());
```

## Routing

- `ui.path()` — current browser path for this run
- `ui.navigate("/x")` — set path for this run (client syncs via `history.replaceState`)
- On connect, the client sends `location.pathname`

## Shell

Prefer `sidebar.brand` / `sidebar.nav` / `sidebar.footer` and optional `ui.header`.
The framework owns chrome layout and tokens; apps declare structure only.

## Widgets

Values stay scoped to the current session. Interactions rerun `build()`.

```java
boolean enabled = ui.checkbox("Enable notifications", true);
double retries = ui.numberInput("Retries", 3.0, 0.0, 10.0, 1.0);
String region = ui.selectbox("Region", List.of("US East", "EU West"));
String plan = ui.radio("Plan", List.of("Free", "Team"), 1);
double volume = ui.slider("Volume", 0.0, 100.0, 50.0, 5.0);
```

`spinner` shows during a blocking operation and clears when it finishes:

```java
ui.spinner("Loading report", () -> ui.text("Report ready."));
```

`downloadButton` starts a browser download and returns `true` on the click rerun
(downloads capped at 1 MiB):

```java
if (ui.downloadButton("Download report", bytes, "report.csv")) {
    ui.text("Download started.");
}
```

## AI and agents

Use `AiProvider` for provider-neutral streaming. Built-in `EchoAiProvider` needs no API keys:

```java
AiProvider provider = new EchoAiProvider();
ui.ai(provider.stream("Summarize Lumina"));
ui.citation("Architecture", "docs/ARCHITECTURE.md", "Server-driven component trees.");
ui.usage(120, 48, 0.001, 80L);
```

`lumina-spring-ai` provides `SpringAiChatClientProvider` for an application-managed Spring AI
`ChatClient`. Configure `lumina.ai.provider=echo`, `openai`, or `ollama` (echo is the safe default).

Agent surfaces: `agentTimeline`, `toolInvocation`, `approval`, and `memoryPanel`.

## Advanced UX

Use `tabs`, `dialog`, `notify`, and `themeToggle`. Tabs expose tablist/tab/tabpanel semantics;
notifications use a polite live region; theme preference lives in session under `__lumina.theme`.

## Enterprise hooks

Store authenticated role names under `__lumina.roles` for `ui.rolesAllowed(roles, body)`.
`AuditLogger` receives session and intent names only (payloads excluded). Store translation maps
under `__lumina.messages` and resolve with `ui.t(key)`.

## Known 1.0 limitations

- OpenAI / Ollama work through Spring AI when configured; cloud provider coverage is not exhaustive.
- Multi-node session clustering is not shipped.
- Server-Sent Events (SSE) as an alternate transport is not shipped.

## UX standards

PRs that touch UI must satisfy
[`docs/UX_CONSTITUTION.md`](UX_CONSTITUTION.md).
