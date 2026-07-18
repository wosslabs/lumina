package io.lumina.session.internal;

import io.lumina.state.StateStore;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SessionStateTest {
    @Test
    void exposesStableAppAndWidgetState() {
        SessionState session = new SessionState();

        assertThat(session.store()).isInstanceOf(StateStore.class).isSameAs(session.store());
        assertThat(session.widgets()).isSameAs(session.widgets());
    }
}
