package io.lumina.session.internal;

import io.lumina.state.StateStore;

/**
 * Aggregates app-owned and widget-owned state for one runtime session.
 */
public final class SessionState {
    private final MapStateStore store = new MapStateStore();
    private final WidgetState widgets = new WidgetState();

    /**
     * Creates empty state for a new session.
     */
    public SessionState() {}

    /**
     * Returns app-owned session state.
     *
     * @return session state store
     */
    public StateStore store() {
        return store;
    }

    /**
     * Returns framework-owned widget state.
     *
     * @return widget state
     */
    public WidgetState widgets() {
        return widgets;
    }
}
