package io.lumina.web;

import io.lumina.runtime.Intent;
import io.lumina.runtime.RunResult;
import io.lumina.runtime.RunSink;
import io.lumina.runtime.SessionHandle;
import io.lumina.runtime.SessionManager;
import java.util.ArrayDeque;
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
    private final Object outboundLock = new Object();
    private final ArrayDeque<String> outbound = new ArrayDeque<>();
    private volatile SessionHandle sessionHandle;
    private volatile Session session;
    private boolean sending;

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
        this.session = session;
        sessionHandle = sessionManager.create();
        sessionHandle.submit(Intent.connect(), sinkFor())
                .whenComplete((result, error) -> reply(result, error));
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
            // Malformed frames are routine and client-triggerable; log at DEBUG (with
            // detail) to avoid noise and log-spam. The client still gets a clean error.
            LOGGER.log(System.Logger.Level.DEBUG, "Rejected invalid WebSocket message", e);
            sendError(INVALID_MESSAGE);
            return;
        }
        sessionHandle.submit(intent, sinkFor())
                .whenComplete((result, error) -> reply(result, error));
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

    private void reply(RunResult result, Throwable error) {
        if (error != null) {
            LOGGER.log(System.Logger.Level.ERROR, "WebSocket intent execution failed", error);
            sendError(APPLICATION_ERROR);
            return;
        }
        if (result.hasError()) {
            LOGGER.log(System.Logger.Level.ERROR, "Lumina application failed: {0}", result.error());
            sendError(APPLICATION_ERROR);
            return;
        }
        String json = result.fullSnapshot()
                ? ProtocolCodec.toSnapshotJson(result.root())
                : ProtocolCodec.toPatchJson(result.patches());
        send(json);
    }

    /**
     * Builds a {@link RunSink} that encodes interim structural results and forwards raw stream
     * frames to the connection's session, so a streaming run's {@code stream} frames and mid-run
     * structural flushes reach the client before the final reply (ADR-006).
     *
     * @return sink delivering interim results and stream frames to the connection's session
     */
    private RunSink sinkFor() {
        return new RunSink() {
            @Override
            public void deliverInterim(RunResult interim) {
                if (interim.hasError()) {
                    // Defensive only: flushBefore never delivers an error result mid-run.
                    sendError(APPLICATION_ERROR);
                    return;
                }
                String json = interim.fullSnapshot()
                        ? ProtocolCodec.toSnapshotJson(interim.root())
                        : ProtocolCodec.toPatchJson(interim.patches());
                send(json);
            }

            @Override
            public void sendFrame(String json) {
                send(json);
            }
        };
    }

    private void sendError(String message) {
        send(ProtocolCodec.toErrorJson(message));
    }

    /**
     * Enqueues {@code json} for delivery, preserving FIFO order across callers.
     *
     * <p>Jetty 12's non-blocking write contract forbids starting a new {@code sendText} before
     * the previous one completes, so all outgoing frames are serialized through {@link #outbound}
     * with at most one send in flight; each send's completion callback dispatches the next queued
     * message. This method may be called concurrently from the session's callback thread (interim
     * results, stream frames) and from the {@code whenComplete} thread of the final reply, so
     * queue mutation and the {@link #sending} flag are guarded by {@link #outboundLock}.
     *
     * @param json message to send
     */
    private void send(String json) {
        synchronized (outboundLock) {
            outbound.add(json);
            if (sending) {
                return;
            }
            sending = true;
        }
        dispatchNext();
    }

    private void dispatchNext() {
        String next;
        synchronized (outboundLock) {
            next = outbound.poll();
            if (next == null) {
                sending = false;
                return;
            }
        }
        session.sendText(next, new Callback() {
            @Override
            public void succeed() {
                dispatchNext();
            }

            @Override
            public void fail(Throwable t) {
                LOGGER.log(System.Logger.Level.DEBUG, "WebSocket send failed; dropping queued frames", t);
                synchronized (outboundLock) {
                    outbound.clear();
                    sending = false;
                }
            }
        });
    }

    private void closeSession() {
        SessionHandle handle = sessionHandle;
        if (handle != null) {
            handle.close();
        }
    }
}
