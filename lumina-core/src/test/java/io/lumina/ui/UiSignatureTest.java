package io.lumina.ui;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumina.ai.TokenStream;
import io.lumina.state.StateStore;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class UiSignatureTest {
    private static final class FakeUi implements Ui, SidebarUi, HeaderUi, NavUi {
        @Override public void pageConfig(PageConfig config) {}
        @Override public String path() { return "/"; }
        @Override public void navigate(String routePath) {}
        @Override public void title(String text) {}
        @Override public void markdown(String md) {}
        @Override public void text(String text) {}
        @Override public boolean button(String label) { return false; }
        @Override public String textInput(String label) { return ""; }
        @Override public boolean checkbox(String label) { return false; }
        @Override public boolean checkbox(String label, boolean value) { return value; }
        @Override public double numberInput(String label) { return 0.0; }
        @Override public double numberInput(String label, double value) { return value; }
        @Override public double numberInput(String label, double value, double min, double max, double step) { return value; }
        @Override public String selectbox(String label, List<String> options) { return options.getFirst(); }
        @Override public String selectbox(String label, List<String> options, int index) { return options.get(index); }
        @Override public String radio(String label, List<String> options) { return options.getFirst(); }
        @Override public String radio(String label, List<String> options, int index) { return options.get(index); }
        @Override public double slider(String label, double min, double max) { return min; }
        @Override public double slider(String label, double min, double max, double value) { return value; }
        @Override public double slider(String label, double min, double max, double value, double step) { return value; }
        @Override public void spinner(String label, Runnable body) { body.run(); }
        @Override public boolean downloadButton(String label, byte[] data, String fileName) { return false; }
        @Override public String chatInput() { return null; }
        @Override public void user(String message) {}
        @Override public void ai(String message) {}

        @Override
        public String ai(TokenStream tokens) {
            StringBuilder sb = new StringBuilder();
            tokens.forEach(sb::append);
            return sb.toString();
        }
        @Override public void citation(String title, String urlOrRef, String snippet) {}
        @Override public void ragSources(List<Map<String, Object>> sources) {}
        @Override public void toolCall(String name, String status, Object input, Object output) {}
        @Override public void usage(long promptTokens, long completionTokens, Double costUsd, Long latencyMs) {}
        @Override public void agentTimeline(List<Map<String, Object>> steps) {}
        @Override public void toolInvocation(String toolName, String status, String detail) {}
        @Override public boolean approval(String prompt) { return false; }
        @Override public void memoryPanel(List<Map<String, Object>> entries) {}
        @Override public void code(String language, String source) {}
        @Override public void json(Object value) {}
        @Override public void table(List<Map<String, Object>> rows) {}
        @Override public void image(String urlOrResource) {}
        @Override public Optional<UploadedFile> fileUpload(String label) { return Optional.empty(); }
        @Override public void progress(double value) {}
        @Override public StateStore state() { return null; }
        @Override public <T> T withKey(String key, Function<Ui, T> block) { return block.apply(this); }
        @Override public void container(Consumer<Ui> body) { body.accept(this); }
        @Override public void columns(int n, Consumer<Ui[]> cols) {
            Ui[] array = new Ui[n];
            java.util.Arrays.fill(array, this);
            cols.accept(array);
        }
        @Override public void tabs(List<String> labels, Consumer<Ui[]> panels) { columns(labels.size(), panels); }
        @Override public void dialog(String title, Consumer<Ui> body) { body.accept(this); }
        @Override public void notify(String message) {}
        @Override public void themeToggle() {}
        @Override public void rolesAllowed(Set<String> roles, Consumer<Ui> body) { body.accept(this); }
        @Override public String t(String key) { return key; }
        @Override public void sidebar(Consumer<SidebarUi> body) { body.accept(this); }
        @Override public void header(Consumer<HeaderUi> body) { body.accept(this); }
        @Override public void brand(Consumer<Ui> body) { body.accept(this); }
        @Override public void nav(Consumer<NavUi> body) { body.accept(this); }
        @Override public void footer(Consumer<Ui> body) { body.accept(this); }
        @Override public void page(String label, String path) {}
        @Override public boolean expander(String label, Consumer<Ui> body) {
            body.accept(this);
            return false;
        }
    }

    @Test
    void fakeUiCompilesAgainstPublicContract() {
        Ui ui = new FakeUi();
        ui.title("x");
    }

    @Test
    void aiTokenStreamReturnsAccumulatedText() {
        Ui ui = new FakeUi();
        assertThat(ui.ai(TokenStream.fromIterable(List.of("a", "b")))).isEqualTo("ab");
    }

    @Test
    void widgetMethodsExposeExpectedReturnValues() {
        Ui ui = new FakeUi();

        assertThat(ui.checkbox("Enabled", true)).isTrue();
        assertThat(ui.numberInput("Retries", 2.0, 0.0, 5.0, 1.0)).isEqualTo(2.0);
        assertThat(ui.selectbox("Region", List.of("US", "EU"), 1)).isEqualTo("EU");
        assertThat(ui.radio("Plan", List.of("Free", "Pro"), 1)).isEqualTo("Pro");
        assertThat(ui.slider("Volume", 0.0, 10.0, 4.0, 0.5)).isEqualTo(4.0);
        assertThat(ui.downloadButton("Export", new byte[] {1}, "export.bin")).isFalse();
    }
}
