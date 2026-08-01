package io.lumina.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumina.LuminaException;
import io.lumina.model.ComponentNode;
import io.lumina.model.ComponentTypes;
import io.lumina.session.internal.SessionState;
import io.lumina.ui.UploadedFile;
import java.util.ArrayList;
import java.util.HashMap;
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
    void jsonAndTableSnapshotMutableInputsRecursively() {
        UiBinder ui = new UiBinder(new SessionState());
        Map<String, Object> jsonEntry = new HashMap<>(Map.of("answer", 42));
        List<Object> jsonItems = new ArrayList<>(List.of(jsonEntry));
        Map<String, Object> json = new HashMap<>(Map.of("items", jsonItems));
        List<Object> cells = new ArrayList<>(List.of("original"));
        Map<String, Object> row = new HashMap<>(Map.of("cells", cells));
        List<Map<String, Object>> rows = new ArrayList<>(List.of(row));

        ui.json(json);
        ui.table(rows);

        jsonEntry.put("answer", 99);
        jsonItems.add("added");
        json.put("extra", true);
        cells.set(0, "changed");
        row.put("extra", true);
        rows.add(Map.of("cells", List.of("added")));

        assertThat(ui.buildRoot().children())
                .extracting(ComponentNode::props)
                .containsExactly(
                        Map.of("value", Map.of("items", List.of(Map.of("answer", 42)))),
                        Map.of("rows", List.of(Map.of("cells", List.of("original")))));
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

    @Test
    void containerNestsChildrenUnderContainerNode() {
        UiBinder ui = new UiBinder(new SessionState());
        ui.container(box -> box.text("inside"));
        ComponentNode root = ui.buildRoot();
        assertThat(root.children()).hasSize(1);
        ComponentNode container = root.children().getFirst();
        assertThat(container.type()).isEqualTo(ComponentTypes.CONTAINER);
        assertThat(container.children()).extracting(ComponentNode::type)
                .containsExactly(ComponentTypes.TEXT);
        assertThat(container.children().getFirst().props()).containsEntry("content", "inside");
    }

    @Test
    void columnsCreatesEqualColumnSlots() {
        UiBinder ui = new UiBinder(new SessionState());
        ui.columns(2, cols -> {
            cols[0].markdown("L");
            cols[1].button("R");
        });
        ComponentNode columns = ui.buildRoot().children().getFirst();
        assertThat(columns.type()).isEqualTo(ComponentTypes.COLUMNS);
        assertThat(columns.props()).containsEntry("count", 2);
        assertThat(columns.children()).hasSize(2);
        assertThat(columns.children()).extracting(ComponentNode::type)
                .containsExactly(ComponentTypes.COLUMN, ComponentTypes.COLUMN);
        assertThat(columns.children().get(0).props()).containsEntry("index", 0);
        assertThat(columns.children().get(1).children()).extracting(ComponentNode::type)
                .containsExactly(ComponentTypes.BUTTON);
    }

    @Test
    void secondSidebarThrows() {
        UiBinder ui = new UiBinder(new SessionState());
        ui.sidebar(s -> s.text("ok"));
        assertThatThrownBy(() -> ui.sidebar(s -> s.text("no")))
                .isInstanceOf(LuminaException.class)
                .hasMessageContaining("Only one sidebar");
    }

    @Test
    void columnsScopeWidgetKeysUnderColumnPath() {
        UiBinder ui = new UiBinder(new SessionState());
        ui.columns(2, cols -> {
            cols[0].button("Left");
            cols[1].button("Right");
        });
        ComponentNode columns = ui.buildRoot().children().getFirst();
        String col0ButtonId = columns.children().get(0).children().getFirst().id();
        String col1ButtonId = columns.children().get(1).children().getFirst().id();
        assertThat(col0ButtonId).contains("/column#");
        assertThat(col1ButtonId).contains("/column#");
        assertThat(col0ButtonId).isNotEqualTo(col1ButtonId);
    }

    @Test
    void expanderReflectsWidgetStateOpenFlag() {
        SessionState session = new SessionState();
        UiBinder ui = new UiBinder(session);
        assertThat(ui.expander("Details", b -> b.text("hidden"))).isFalse();
        String expanderId = ui.buildRoot().children().getFirst().id();
        session.widgets().set(expanderId, true);
        ui = new UiBinder(session);
        assertThat(ui.expander("Details", b -> b.text("shown"))).isTrue();
        assertThat(ui.buildRoot().children().getFirst().props()).containsEntry("open", true);
    }

    @Test
    void remainingWidgetsReadStateAndEmitValidatedProps() {
        SessionState session = new SessionState();
        session.widgets().set("auto:/checkbox#0", true);
        session.widgets().set("auto:/number_input#0", 2);
        session.widgets().set("auto:/selectbox#0", "EU");
        session.widgets().set("auto:/radio#0", "Pro");
        session.widgets().set("auto:/slider#0", 7.5);
        session.widgets().set("auto:/download_button#0", true);
        UiBinder ui = new UiBinder(session);

        assertThat(ui.checkbox("Enabled")).isTrue();
        assertThat(ui.numberInput("Retries", 1.0, 0.0, 5.0, 0.5)).isEqualTo(2.0);
        assertThat(ui.selectbox("Region", List.of("US", "EU"))).isEqualTo("EU");
        assertThat(ui.radio("Plan", List.of("Free", "Pro"))).isEqualTo("Pro");
        assertThat(ui.slider("Volume", 0.0, 10.0, 5.0, 0.5)).isEqualTo(7.5);
        assertThat(ui.downloadButton("Export", new byte[] {1, 2}, "export.bin")).isTrue();

        assertThat(ui.buildRoot().children()).extracting(ComponentNode::type).containsExactly(
                ComponentTypes.CHECKBOX,
                ComponentTypes.NUMBER_INPUT,
                ComponentTypes.SELECTBOX,
                ComponentTypes.RADIO,
                ComponentTypes.SLIDER,
                ComponentTypes.DOWNLOAD_BUTTON);
        assertThat(ui.buildRoot().children().get(1).props()).containsEntry("min", 0.0).containsEntry("step", 0.5);
        assertThat(ui.buildRoot().children().get(5).props())
                .containsEntry("fileName", "export.bin")
                .containsEntry("data", "AQI=");
        assertThat(ui.downloadButton("Next", new byte[0], "next.bin")).isFalse();
    }

    @Test
    void remainingWidgetsRejectInvalidConfiguration() {
        UiBinder ui = new UiBinder(new SessionState());

        assertThatThrownBy(() -> ui.selectbox("Empty", List.of())).isInstanceOf(LuminaException.class);
        assertThatThrownBy(() -> ui.radio("Plan", List.of("Free"), 1)).isInstanceOf(LuminaException.class);
        assertThatThrownBy(() -> ui.slider("Volume", 2.0, 1.0)).isInstanceOf(LuminaException.class);
        assertThatThrownBy(() -> ui.downloadButton("Large", new byte[1024 * 1024 + 1], "large.bin"))
                .isInstanceOf(LuminaException.class);
    }

    @Test
    void aiAndAgentExtrasEmitFrameworkComponentNodes() {
        SessionState session = new SessionState();
        session.widgets().set("auto:/approval#0", true);
        UiBinder ui = new UiBinder(session);

        ui.citation("Guide", "guide", "snippet");
        ui.ragSources(List.of(Map.of("title", "Guide")));
        ui.toolCall("search", "complete", Map.of("q", "x"), Map.of("count", 1));
        ui.usage(4, 2, null, 10L);
        ui.agentTimeline(List.of(Map.of("label", "Plan", "status", "complete")));
        ui.toolInvocation("search", "complete", "done");
        assertThat(ui.approval("Continue?")).isTrue();
        ui.memoryPanel(List.of(Map.of("topic", "Lumina")));

        assertThat(ui.buildRoot().children()).extracting(ComponentNode::type).containsExactly(
                ComponentTypes.CITATION,
                ComponentTypes.RAG_SOURCES,
                ComponentTypes.TOOL_CALL,
                ComponentTypes.USAGE,
                ComponentTypes.AGENT_TIMELINE,
                ComponentTypes.TOOL_INVOCATION,
                ComponentTypes.APPROVAL,
                ComponentTypes.MEMORY_PANEL);
    }

    @Test
    void tabsThemeAndRoleGateUseSessionState() {
        SessionState session = new SessionState();
        session.store().set("__lumina.roles", java.util.Set.of("admin"));
        session.widgets().set("auto:/theme_toggle#0", "dark");
        UiBinder ui = new UiBinder(session);

        ui.tabs(List.of("One", "Two"), panels -> panels[0].text("first"));
        ui.themeToggle();
        ui.rolesAllowed(java.util.Set.of("admin"), body -> body.text("allowed"));

        assertThat(ui.buildRoot().children()).extracting(ComponentNode::type)
                .containsExactly(ComponentTypes.TABS, ComponentTypes.THEME_TOGGLE, ComponentTypes.TEXT);
        assertThat((String) session.store().get("__lumina.theme")).isEqualTo("dark");
    }

    private record NamedValue(String value) {
        @Override
        public String toString() {
            return value;
        }
    }
}
