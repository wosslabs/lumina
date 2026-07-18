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
    private final SessionManager sessionManager;
    private volatile SessionHandle sessionHandle;

    LuminaWebSocketEndpoint(SessionManager sessionManager) {
        this.sessionManager = Objects.requireNonNull(sessionManager, "sessionManager");
    }

    @OnWebSocketOpen
    public void onOpen(Session session) {
        sessionHandle = sessionManager.create();
        sessionHandle.submit(Intent.connect()).whenComplete((result, error) -> reply(session, result, error));
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        Intent intent;
        try {
            intent = ProtocolCodec.parseIntent(message);
        } catch (RuntimeException e) {
            sendError(session, describe(e));
            return;
        }
        sessionHandle.submit(intent).whenComplete((result, error) -> reply(session, result, error));
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        closeSession();
    }

    @OnWebSocketError
    public void onError(Throwable cause) {
        closeSession();
    }

    private void reply(Session session, RunResult result, Throwable error) {
        if (error != null) {
            sendError(session, describe(error));
            return;
        }
        if (result.hasError()) {
            sendError(session, result.error());
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

    private String describe(Throwable t) {
        String message = t.getMessage();
        return message != null ? message : t.getClass().getSimpleName();
    }
}
