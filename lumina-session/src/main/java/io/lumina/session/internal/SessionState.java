package io.lumina.session.internal;

import io.lumina.state.StateStore;

public final class SessionState {
    private final MapStateStore store = new MapStateStore();
    private final WidgetState widgets = new WidgetState();

    public StateStore store() {
        return store;
    }

    public WidgetState widgets() {
        return widgets;
    }
}
