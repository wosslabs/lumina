package io.lumina.web;

import io.lumina.runtime.Intent;
import io.lumina.runtime.RunResult;
import io.lumina.runtime.SessionHandle;
import io.lumina.runtime.SessionManager;
import java.util.Objects;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketOpen;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

/**
 * One WebSocket connection at {@code /ws}: creates a session on open, sends the initial
 * connect snapshot, and thereafter decodes each incoming message as an {@link Intent}, submits
 * it, and replies with the resulting patch or an error (ADR-003, ADR-005).
 *
 * <p>A new instance is created per connection by {@link JettyLuminaHttpServer}, so
 * {@link #sessionHandle} is confined to this connection's session.
 */
@WebSocket
public final class LuminaWebSocketEndpoint {
    private static final System.Logger LOGGER =
            System.getLogger(LuminaWebSocketEndpoint.class.getName());
    private static final String INVALID_MESSAGE = "Invalid message";
    private static final String APPLICATION_ERROR = "Application error";

    private final SessionManager sessionManager;
    private volatile SessionHandle sessionHandle;

    LuminaWebSocketEndpoint(SessionManager sessionManager) {
        this.sessionManager = Objects.requireNonNull(sessionManager, "sessionManager");
    }

    /**
     * Opens a Lumina session and sends its initial component-tree snapshot.
     *
     * @param session newly opened WebSocket session
     */
    @OnWebSocketOpen
    public void onOpen(Session session) {
        sessionHandle = sessionManager.create();
        sessionHandle.submit(Intent.connect()).whenComplete((result, error) -> reply(session, result, error));
    }

    /**
     * Applies a browser intent and sends the resulting patch or error.
     *
     * @param session active WebSocket session
     * @param message JSON intent message
     */
    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        Intent intent;
        try {
            intent = ProtocolCodec.parseIntent(message);
        } catch (RuntimeException e) {
            LOGGER.log(System.Logger.Level.WARNING, "Rejected invalid WebSocket message", e);
            sendError(session, INVALID_MESSAGE);
            return;
        }
        sessionHandle.submit(intent).whenComplete((result, error) -> reply(session, result, error));
    }

    /**
     * Releases the Lumina session after its WebSocket closes.
     *
     * @param statusCode WebSocket close status
     * @param reason peer-supplied close reason
     */
    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        closeSession();
    }

    /**
     * Releases the Lumina session after a WebSocket failure.
     *
     * @param cause WebSocket failure
     */
    @OnWebSocketError
    public void onError(Throwable cause) {
        closeSession();
    }

    private void reply(Session session, RunResult result, Throwable error) {
        if (error != null) {
            LOGGER.log(System.Logger.Level.ERROR, "WebSocket intent execution failed", error);
            sendError(session, APPLICATION_ERROR);
            return;
        }
        if (result.hasError()) {
            LOGGER.log(System.Logger.Level.ERROR, "Lumina application failed: {0}", result.error());
            sendError(session, APPLICATION_ERROR);
            return;
        }
        String json = result.fullSnapshot()
                ? ProtocolCodec.toSnapshotJson(result.root())
                : ProtocolCodec.toPatchJson(result.patches());
        session.sendText(json, Callback.NOOP);
    }

    private void sendError(Session session, String message) {
        session.sendText(ProtocolCodec.toErrorJson(message), Callback.NOOP);
    }

    private void closeSession() {
        SessionHandle handle = sessionHandle;
        if (handle != null) {
            handle.close();
        }
    }
}
