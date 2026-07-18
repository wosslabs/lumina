package io.lumina.web;

import io.lumina.LuminaApp;
import io.lumina.runtime.SessionManager;
import java.net.URI;
import java.util.Objects;

/**
 * Public bootstrap for embedding Lumina: starts the embedded server from ADR-005 for one
 * {@link LuminaApp} and serves ADR-003's snapshot/patch/error protocol over WebSocket.
 */
public final class LuminaServer {
    private final LuminaHttpServer httpServer;

    private LuminaServer(LuminaHttpServer httpServer) {
        this.httpServer = httpServer;
    }

    /**
     * Starts a server for {@code app} with default configuration (all interfaces, port 8080).
     *
     * @param app application entry point
     * @return running server
     */
    public static LuminaServer start(LuminaApp app) {
        return start(app, LuminaServerConfig.defaults());
    }

    /**
     * Starts a server for {@code app} with the given configuration.
     *
     * @param app application entry point
     * @param config server configuration
     * @return running server
     */
    public static LuminaServer start(LuminaApp app, LuminaServerConfig config) {
        Objects.requireNonNull(app, "app");
        Objects.requireNonNull(config, "config");
        SessionManager sessionManager = new SessionManager(app);
        LuminaHttpServer httpServer = new JettyLuminaHttpServer(sessionManager, config);
        httpServer.start();
        return new LuminaServer(httpServer);
    }

    /**
     * Returns the bound port, resolved from the OS when the configured port was {@code 0}.
     *
     * @return bound TCP port
     */
    public int port() {
        return httpServer.port();
    }

    /**
     * Returns the base URI browsers should use to reach this server.
     *
     * @return base HTTP URI
     */
    public URI uri() {
        return httpServer.uri();
    }

    /**
     * Stops serving requests and releases server resources.
     */
    public void stop() {
        httpServer.stop();
    }
}
