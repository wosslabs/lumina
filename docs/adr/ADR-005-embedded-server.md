# ADR-005: Embedded HTTP/WebSocket server

## Status
Accepted

## Context
Lumina apps need a zero-config way to serve the browser client and exchange the JSON protocol
from ADR-003 over WebSocket, without forcing `lumina-core` or `lumina-runtime` to depend on a
servlet container. ADR-001 confines Jetty and Servlet APIs to `lumina-web`.

## Decision
`lumina-web` embeds Jetty 12 (`jetty-server`, `jetty-ee10-servlet`,
`jetty-ee10-websocket-jetty-server`) behind a package-private `LuminaHttpServer` abstraction so
the concrete server implementation (`JettyLuminaHttpServer`) stays swappable without touching the
public API. `LuminaServer` is the sole public bootstrap:

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

`LuminaServer.start` creates one `SessionManager` for the app, starts the embedded server, and
binds three routes: `GET /` serves a static `index.html` shell, `GET /lumina-web/**` serves other
static assets (browser client, Task 9), and `/ws` upgrades to a WebSocket handled by
`LuminaWebSocketEndpoint`. On WebSocket open, the endpoint creates one session
(`SessionManager.create()`), runs `Intent.connect()`, and sends the resulting snapshot. On each
incoming message, it decodes an intent with `ProtocolCodec`, submits it to the session, and sends
the resulting patch — or an error message if decoding or the rerun failed. `ProtocolCodec` is the
single place that encodes and decodes the ADR-003 wire shapes with Jackson, using Jackson's native
record support so `ComponentNode`/`PatchOp` require no hand-written (de)serializers.

`LuminaServerConfig` carries `host` (default `127.0.0.1`, loopback-only) and `port` (default
`8080`); tests bind port `0` and read the OS-assigned port back from the running server via
`LuminaServer.port()`. It also carries `maxSessions` (default `100`) and `idleTimeout` (default
30 minutes) enforced at the WebSocket upgrade handshake, and an optional `allowedOrigins`
allowlist.

The upgrade handshake rejects requests before a `LuminaWebSocketEndpoint` is even created when:
the `Origin` header is present but does not match the server's own host/localhost on the bound
port (or `allowedOrigins`, if configured) — mitigating cross-site WebSocket hijacking, since a
browser always sends `Origin` while non-browser clients typically omit it; or the number of open
sessions is already at `maxSessions` — the client receives an HTTP error response instead of an
upgraded connection.

## Consequences
Apps embed Lumina with one call (`LuminaServer.start(app)`) without knowing Jetty exists.
`lumina-core` and `lumina-runtime` remain servlet- and Jetty-free per ADR-001; Jetty and Servlet
types appear only inside `lumina-web`. Swapping the embedded HTTP server later (e.g. Netty) only
requires a new `LuminaHttpServer` implementation — `LuminaServer`, `ProtocolCodec`, and the public
API are unaffected. One `SessionHandle` is created per WebSocket connection, so a reconnect always
starts a fresh session; session resumption across reconnects is out of scope for Phase 1.
