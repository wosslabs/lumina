package io.lumina.web;

import io.lumina.LuminaException;
import io.lumina.runtime.SessionManager;
import java.net.URI;
import java.util.Objects;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.ee10.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

/**
 * Jetty 12 implementation of {@link LuminaHttpServer} (ADR-005): one {@link ServletContextHandler}
 * serving the static client shell at {@code /}, static assets at {@code /lumina-web/**}, and a
 * WebSocket upgrade at {@code /ws} that creates a fresh {@link LuminaWebSocketEndpoint} per
 * connection.
 */
final class JettyLuminaHttpServer implements LuminaHttpServer {
    private final LuminaServerConfig config;
    private final Server server;
    private final ServerConnector connector;

    JettyLuminaHttpServer(SessionManager sessionManager, LuminaServerConfig config) {
        Objects.requireNonNull(sessionManager, "sessionManager");
        this.config = Objects.requireNonNull(config, "config");

        this.server = new Server();
        this.connector = new ServerConnector(server);
        connector.setHost(config.host());
        connector.setPort(config.port());
        server.addConnector(connector);

        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        context.addServlet(new ServletHolder(new IndexServlet()), "/");
        context.addServlet(new ServletHolder(new StaticResourceServlet()), "/lumina-web/*");
        JettyWebSocketServletContainerInitializer.configure(context, (servletContext, container) ->
                container.addMapping("/ws", (request, response) -> new LuminaWebSocketEndpoint(sessionManager)));
        server.setHandler(context);
    }

    @Override
    public void start() {
        try {
            server.start();
        } catch (Exception e) {
            throw new LuminaException("Failed to start Lumina server", e);
        }
    }

    @Override
    public void stop() {
        try {
            server.stop();
        } catch (Exception e) {
            throw new LuminaException("Failed to stop Lumina server", e);
        }
    }

    @Override
    public int port() {
        return connector.getLocalPort();
    }

    @Override
    public URI uri() {
        return URI.create("http://" + displayHost() + ":" + port() + "/");
    }

    private String displayHost() {
        String host = config.host();
        return host.isBlank() || host.equals("0.0.0.0") ? "localhost" : host;
    }
}
