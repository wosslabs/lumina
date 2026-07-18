package io.lumina.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumina.model.ComponentNode;
import io.lumina.model.ComponentTypes;
import io.lumina.session.internal.SessionState;
import io.lumina.ui.UploadedFile;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class UiBinderTest {
    @Test
    void buildEmitsAllDisplayComponentNodes() {
        UiBinder ui = new UiBinder(new SessionState());
        List<Map<String, Object>> rows = List.of(Map.of("answer", 42));

        ui.title("Title");
        ui.markdown("**bold**");
        ui.text("plain");
        ui.user("question");
        ui.ai("answer");
        ui.code("java", "record Example() {}");
        ui.json(Map.of("ready", true));
        ui.json(new NamedValue("fallback"));
        ui.table(rows);
        ui.image("/logo.svg");
        ui.progress(0.75);

        assertThat(ui.buildRoot().children())
                .extracting(ComponentNode::type)
                .containsExactly(
                        ComponentTypes.TITLE,
                        ComponentTypes.MARKDOWN,
                        ComponentTypes.TEXT,
                        ComponentTypes.USER_MESSAGE,
                        ComponentTypes.AI_MESSAGE,
                        ComponentTypes.CODE,
                        ComponentTypes.JSON,
                        ComponentTypes.JSON,
                        ComponentTypes.TABLE,
                        ComponentTypes.IMAGE,
                        ComponentTypes.PROGRESS);
        assertThat(ui.buildRoot().children())
                .extracting(ComponentNode::props)
                .containsExactly(
                        Map.of("content", "Title"),
                        Map.of("content", "**bold**"),
                        Map.of("content", "plain"),
                        Map.of("content", "question"),
                        Map.of("content", "answer"),
                        Map.of("language", "java", "source", "record Example() {}"),
                        Map.of("value", Map.of("ready", true)),
                        Map.of("value", "fallback"),
                        Map.of("rows", rows),
                        Map.of("src", "/logo.svg"),
                        Map.of("value", 0.75));
    }

    @Test
    void interactiveComponentsUseKeysAndReadWidgetState() {
        SessionState session = new SessionState();
        session.widgets().set("auto:/button#0", true);
        session.widgets().set("auto:/text_input#0", "Ada");
        session.widgets().setChatSubmit("auto:/chat_input#0", "hello");
        UploadedFile upload = new UploadedFile("notes.txt", "text/plain", new byte[] {1});
        session.widgets().set("auto:/file_upload#0", upload);
        UiBinder ui = new UiBinder(session);

        assertThat(ui.button("Save")).isTrue();
        assertThat(ui.textInput("Name")).isEqualTo("Ada");
        assertThat(ui.chatInput()).isEqualTo("hello");
        assertThat(ui.fileUpload("Notes")).containsSame(upload);

        assertThat(ui.buildRoot().children())
                .extracting(ComponentNode::id)
                .containsExactly(
                        "auto:/button#0",
                        "auto:/text_input#0",
                        "auto:/chat_input#0",
                        "auto:/file_upload#0");
        assertThat(ui.buildRoot().children())
                .extracting(ComponentNode::props)
                .containsExactly(
                        Map.of("label", "Save"),
                        Map.of("label", "Name", "value", "Ada"),
                        Map.of(),
                        Map.of("label", "Notes"));
        assertThat(session.widgets().consumeClick("auto:/button#0")).isFalse();
        assertThat(session.widgets().consumeChatSubmit("auto:/chat_input#0")).isNull();
        assertThat((Object) session.widgets().value("auto:/file_upload#0")).isNull();
    }

    @Test
    void missingWidgetValuesReturnDefaults() {
        UiBinder ui = new UiBinder(new SessionState());

        assertThat(ui.button("Save")).isFalse();
        assertThat(ui.textInput("Name")).isEmpty();
        assertThat(ui.chatInput()).isNull();
        assertThat(ui.fileUpload("Notes")).isEmpty();
    }

    @Test
    void autoKeysCountEachTypeIndependently() {
        UiBinder ui = new UiBinder(new SessionState());

        ui.button("First");
        ui.textInput("Name");
        ui.button("Second");

        assertThat(ui.buildRoot().children())
                .extracting(ComponentNode::id)
                .containsExactly("auto:/button#0", "auto:/text_input#0", "auto:/button#1");
    }

    @Test
    void withKeyScopesKeysAndReturnsBlockResult() {
        UiBinder ui = new UiBinder(new SessionState());

        String result = ui.withKey("profile", nested -> {
            nested.textInput("First");
            nested.button("Save");
            return "done";
        });
        ui.textInput("Outside");

        assertThat(result).isEqualTo("done");
        assertThat(ui.buildRoot().children())
                .extracting(ComponentNode::id)
                .containsExactly(
                        "auto:/profile/text_input#0",
                        "auto:/profile/button#0",
                        "auto:/text_input#0");
    }

    @Test
    void stateReturnsSessionStore() {
        SessionState session = new SessionState();
        UiBinder ui = new UiBinder(session);

        assertThat(ui.state()).isSameAs(session.store());
    }

    @Test
    void progressRejectsValuesOutsideInclusiveRange() {
        UiBinder ui = new UiBinder(new SessionState());

        assertThatThrownBy(() -> ui.progress(-0.01)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ui.progress(1.01)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ui.progress(Double.NaN)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void runResultExposesRoot() {
        ComponentNode root = new ComponentNode("root", ComponentTypes.ROOT, Map.of(), List.of());

        assertThat(new RunResult(root).root()).isSameAs(root);
    }

    private record NamedValue(String value) {
        @Override
        public String toString() {
            return value;
        }
    }
}
