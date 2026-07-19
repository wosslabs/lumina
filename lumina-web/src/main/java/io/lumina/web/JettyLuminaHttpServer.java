package io.lumina.web;

import io.lumina.LuminaException;
import io.lumina.runtime.SessionManager;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.ee11.websocket.server.JettyServerUpgradeRequest;
import org.eclipse.jetty.ee11.websocket.server.JettyServerUpgradeResponse;
import org.eclipse.jetty.ee11.websocket.server.JettyWebSocketServerContainer;
import org.eclipse.jetty.ee11.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

/**
 * Jetty 12 implementation of {@link LuminaHttpServer} (ADR-005): one {@link ServletContextHandler}
 * serving the static client shell at {@code /}, static assets at {@code /lumina-web/**}, and a
 * WebSocket upgrade at {@code /ws} that creates a fresh {@link LuminaWebSocketEndpoint} per
 * connection.
 *
 * <p>The upgrade handshake rejects cross-site requests (checking {@code Origin} against the
 * server's own host/localhost, or {@link LuminaServerConfig#allowedOrigins()} when configured)
 * and connections beyond {@link LuminaServerConfig#maxSessions()}, mitigating cross-site
 * WebSocket hijacking and unbounded session growth.
 */
final class JettyLuminaHttpServer implements LuminaHttpServer {
    /** Text message frame size: 1 MB uploads plus base64 (~1.37x) and JSON envelope overhead. */
    private static final long MAX_TEXT_MESSAGE_SIZE = 2L * 1024 * 1024;

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
        JettyWebSocketServletContainerInitializer.configure(context, (servletContext, container) -> {
            container.setMaxTextMessageSize(MAX_TEXT_MESSAGE_SIZE);
            container.setIdleTimeout(config.idleTimeout());
            container.addMapping("/ws", (request, response) -> createEndpoint(sessionManager, container, request, response));
        });
        server.setHandler(context);
    }

    private Object createEndpoint(
            SessionManager sessionManager,
            JettyWebSocketServerContainer container,
            JettyServerUpgradeRequest request,
            JettyServerUpgradeResponse response)
            throws IOException {
        if (!isOriginAllowed(request.getOrigin())) {
            response.sendForbidden("Origin not allowed");
            return null;
        }
        if (container.getOpenSessions().size() >= config.maxSessions()) {
            response.sendError(503, "Server has reached its concurrent session limit");
            return null;
        }
        return new LuminaWebSocketEndpoint(sessionManager);
    }

    /**
     * Checks a WebSocket handshake's {@code Origin} header against the configured allowlist, or
     * the server's own host and localhost aliases on the bound port when none is configured.
     * Requests without an {@code Origin} header (non-browser clients) are always allowed, since
     * cross-site WebSocket hijacking relies on a browser sending that header automatically.
     */
    private boolean isOriginAllowed(String origin) {
        if (origin == null || origin.isBlank()) {
            return true;
        }
        if (!config.allowedOrigins().isEmpty()) {
            return config.allowedOrigins().contains(origin);
        }
        return isSameHostOrigin(origin);
    }

    private boolean isSameHostOrigin(String origin) {
        URI originUri;
        try {
            originUri = new URI(origin);
        } catch (URISyntaxException e) {
            return false;
        }
        String originHost = originUri.getHost();
        if (originHost == null) {
            return false;
        }
        int originPort = originUri.getPort() == -1 ? defaultPortFor(originUri.getScheme()) : originUri.getPort();
        if (originPort != port()) {
            return false;
        }
        return isLocalhostAlias(originHost) || originHost.equalsIgnoreCase(config.host());
    }

    private static boolean isLocalhostAlias(String host) {
        return host.equalsIgnoreCase("localhost") || host.equals("127.0.0.1") || host.equals("::1") || host.equals("[::1]");
    }

    private static int defaultPortFor(String scheme) {
        return "https".equalsIgnoreCase(scheme) || "wss".equalsIgnoreCase(scheme) ? 443 : 80;
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
