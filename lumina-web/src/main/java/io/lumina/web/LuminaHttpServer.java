package io.lumina.web;

import java.net.URI;

/**
 * Abstraction over the embedded HTTP/WebSocket server (ADR-005), so the concrete server
 * technology (currently Jetty, see {@link JettyLuminaHttpServer}) can change without affecting
 * the public {@link LuminaServer} API.
 */
interface LuminaHttpServer {
    /**
     * Starts serving requests. Blocks until the server is ready to accept connections.
     */
    void start();

    /**
     * Stops serving requests and releases server resources.
     */
    void stop();

    /**
     * Returns the bound port, resolved from the OS when the configured port was {@code 0}.
     *
     * @return bound TCP port
     */
    int port();

    /**
     * Returns the base URI apps and browsers should use to reach this server.
     *
     * @return base HTTP URI
     */
    URI uri();
}
