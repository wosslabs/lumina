package io.lumina.runtime;

import io.lumina.LuminaApp;
import io.lumina.session.internal.SessionState;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * One user session: owns its own state and a serial execution queue, so intents for this session
 * never run concurrently while intents for other sessions proceed independently.
 */
public final class SessionHandle {
    private final LuminaApp app;
    private final SessionState session = new SessionState();
    private final AppRunner runner = new AppRunner();
    private final SessionExecutor executor = new SessionExecutor();

    SessionHandle(LuminaApp app) {
        this.app = Objects.requireNonNull(app, "app");
    }

    /**
     * Queues {@code intent} for this session and reruns the app once prior submissions complete.
     *
     * @param intent intent to apply
     * @return future completed with the run result, or completed exceptionally on a framework error
     */
    public CompletableFuture<RunResult> submit(Intent intent) {
        Objects.requireNonNull(intent, "intent");
        return executor.submit(() -> runner.run(app, session, intent));
    }

    /**
     * Queues {@code intent} for this session and reruns the app once prior submissions complete,
     * streaming interim structural results and text {@code stream} frames to {@code sink} as the
     * rerun progresses.
     *
     * @param intent intent to apply
     * @param sink receiver of interim results and stream frames
     * @return future completed with the run result, or completed exceptionally on a framework error
     */
    public CompletableFuture<RunResult> submit(Intent intent, RunSink sink) {
        Objects.requireNonNull(intent, "intent");
        Objects.requireNonNull(sink, "sink");
        return executor.submit(() -> runner.run(app, session, intent, sink));
    }

    /**
     * Stops this session's executor thread. No further submissions will be processed.
     */
    public void close() {
        executor.shutdown();
    }
}
