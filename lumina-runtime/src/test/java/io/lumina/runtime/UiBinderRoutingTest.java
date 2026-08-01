package io.lumina.runtime;

import static io.lumina.components.ComponentSpecs.PATH;
import static org.assertj.core.api.Assertions.assertThat;

import io.lumina.session.internal.SessionState;
import org.junit.jupiter.api.Test;

class UiBinderRoutingTest {

    @Test
    void pathDefaultsToRoot() {
        UiBinder ui = new UiBinder(new SessionState());
        ui.title("Home");
        assertThat(ui.path()).isEqualTo("/");
        assertThat(ui.buildRoot().props()).containsEntry(PATH, "/");
    }

    @Test
    void navigateUpdatesPathForSameRunAndRootProps() {
        UiBinder ui = new UiBinder(new SessionState());
        ui.navigate("/about");
        assertThat(ui.path()).isEqualTo("/about");
        ui.title("About");
        assertThat(ui.buildRoot().props()).containsEntry(PATH, "/about");
    }
}
