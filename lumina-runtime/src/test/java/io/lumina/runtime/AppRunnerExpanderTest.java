package io.lumina.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumina.LuminaApp;
import io.lumina.session.internal.SessionState;
import org.junit.jupiter.api.Test;

class AppRunnerExpanderTest {
    @Test
    void expanderToggleFlipsOpenOnNextRun() {
        SessionState session = new SessionState();
        AppRunner runner = new AppRunner();
        LuminaApp app = ui -> ui.expander("More", body -> body.text("x"));

        runner.run(app, session, Intent.connect());
        String expanderId = runner.run(app, session, Intent.connect()).root().children().getFirst().id();
        assertThat(runner.run(app, session, Intent.connect()).root().children().getFirst().props())
                .containsEntry("open", false);

        runner.run(app, session, Intent.expanderToggle(expanderId));
        assertThat(runner.run(app, session, Intent.connect()).root().children().getFirst().props())
                .containsEntry("open", true);
    }
}
