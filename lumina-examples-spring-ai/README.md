# Path C — Spring Boot + Spring AI + Lumina

Streaming chat demo: Tomcat on **8080**, Lumina Jetty on **8090**.

## Run (offline echo — no API key)

```bash
env -u SPRING_CONFIG_IMPORT mvn -q -pl lumina-examples-spring-ai -am spring-boot:run
```

Open **[http://127.0.0.1:8090/](http://127.0.0.1:8090/)** — not `:8080`.

Without `OPENAI_API_KEY`, the app starts in **offline echo** mode (`ChatClients.echo()`).
The header shows a note; replies are prefixed with `Echo:`.

## Run with OpenAI

```bash
export OPENAI_API_KEY=sk-...
env -u SPRING_CONFIG_IMPORT mvn -q -pl lumina-examples-spring-ai -am spring-boot:run
```

When Spring AI registers a `ChatModel` bean, `lumina-spring-ai` auto-configures
`SpringAiChatClient` for live streaming.

!!! warning "Disconnected on :8080"
    With `spring-boot-starter-webmvc`, Tomcat listens on `server.port` (8080). It may serve
    Lumina static assets from the classpath, but **has no `/ws` WebSocket** — the UI shows
    **Disconnected**. That is expected. Use Lumina's port (`8090`) for the full UI.
