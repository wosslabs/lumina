# Path B — Spring Boot + Lumina

Minimal greet demo: Tomcat on **8080**, Lumina Jetty on **8090**.

```bash
env -u SPRING_CONFIG_IMPORT mvn -q -pl lumina-examples-spring -am spring-boot:run
```

Open **[http://127.0.0.1:8090/](http://127.0.0.1:8090/)** — not `:8080`.

!!! warning "Disconnected on :8080"
    With `spring-boot-starter-webmvc`, Tomcat listens on `server.port` (8080). It may serve
    Lumina static assets from the classpath, but **has no `/ws` WebSocket** — the UI shows
    **Disconnected**. That is expected. Use Lumina's port (`8090`) for the full UI.
