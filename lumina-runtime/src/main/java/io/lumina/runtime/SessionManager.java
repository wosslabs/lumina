package io.lumina.runtime;

import io.lumina.LuminaApp;
import java.util.Objects;

/**
 * Creates sessions for one {@link LuminaApp}. Each session is fully isolated: its own widget
 * state, app-owned store, and serial rerun queue.
 */
public final class SessionManager {
    private final LuminaApp app;

    /**
     * Creates a manager that spawns sessions running {@code app}.
     *
     * @param app application entry point invoked on each session rerun
     */
    public SessionManager(LuminaApp app) {
        this.app = Objects.requireNonNull(app, "app");
    }

    /**
     * Creates a new, isolated session.
     *
     * @return new session handle
     */
    public SessionHandle create() {
        return new SessionHandle(app);
    }
}
