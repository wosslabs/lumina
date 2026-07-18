package io.lumina.devtools;

/**
 * Placeholder {@link ReloadSpi} that ignores change notifications until hot-reload
 * support is implemented.
 */
public final class NoOpReloader implements ReloadSpi {
    @Override
    public void onChange(Runnable rebuild) {
        // Phase 1 skeleton: no classpath watching yet.
    }
}
