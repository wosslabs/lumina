package io.lumina.devtools;

/**
 * Service provider interface for hot-reload hooks. Phase 1 provides a no-op
 * implementation; full classpath watching arrives in a later release.
 */
@FunctionalInterface
public interface ReloadSpi {
    /**
     * Registers a callback to invoke when application classes change.
     *
     * @param rebuild action that rebuilds and restarts the running app
     */
    void onChange(Runnable rebuild);
}
