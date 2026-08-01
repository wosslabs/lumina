package io.lumina.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumina.LuminaApp;
import io.lumina.session.internal.SessionState;
import org.junit.jupiter.api.Test;

class AppRunnerRoutingTest {

    @Test
    void connectIntentSetsInitialPath() {
        SessionState session = new SessionState();
        AppRunner runner = new AppRunner();
        LuminaApp app = ui -> ui.title("At " + ui.path());

        RunResult result = runner.run(app, session, Intent.connectWithPath("/docs"));

        assertThat(result.root().props()).containsEntry("path", "/docs");
        assertThat(result.root().children().getFirst().props().get("content")).isEqualTo("At /docs");
    }

    @Test
    void navigateIntentChangesPathOnRerun() {
        SessionState session = new SessionState();
        AppRunner runner = new AppRunner();
        LuminaApp app = ui -> ui.title("At " + ui.path());

        runner.run(app, session, Intent.connect());
        RunResult result = runner.run(app, session, Intent.navigate("/settings"));

        assertThat(result.root().props()).containsEntry("path", "/settings");
        assertThat(result.root().children().getFirst().props().get("content")).isEqualTo("At /settings");
    }
}
