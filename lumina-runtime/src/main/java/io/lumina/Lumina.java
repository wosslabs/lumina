package io.lumina;

import io.lumina.runtime.SessionManager;

/**
 * Convenience entry points for embedding Lumina without a specific transport.
 */
public final class Lumina {
    private Lumina() {}

    /**
     * Creates a session manager for {@code app}. Each call to
     * {@link SessionManager#create()} produces an isolated, headless session that can be driven
     * with {@link io.lumina.runtime.Intent} submissions.
     *
     * @param app application entry point
     * @return new session manager
     */
    public static SessionManager sessionManager(LuminaApp app) {
        return new SessionManager(app);
    }
}
