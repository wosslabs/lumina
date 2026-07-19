package io.lumina.ui;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumina.ai.TokenStream;
import io.lumina.state.StateStore;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class UiSignatureTest {
    private static final class FakeUi implements Ui {
        @Override public void title(String text) {}
        @Override public void markdown(String md) {}
        @Override public void text(String text) {}
        @Override public boolean button(String label) { return false; }
        @Override public String textInput(String label) { return ""; }
        @Override public String chatInput() { return null; }
        @Override public void user(String message) {}
        @Override public void ai(String message) {}

        @Override
        public String ai(TokenStream tokens) {
            StringBuilder sb = new StringBuilder();
            tokens.forEach(sb::append);
            return sb.toString();
        }
        @Override public void code(String language, String source) {}
        @Override public void json(Object value) {}
        @Override public void table(List<Map<String, Object>> rows) {}
        @Override public void image(String urlOrResource) {}
        @Override public Optional<UploadedFile> fileUpload(String label) { return Optional.empty(); }
        @Override public void progress(double value) {}
        @Override public StateStore state() { return null; }
        @Override public <T> T withKey(String key, Function<Ui, T> block) { return block.apply(this); }
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
}
