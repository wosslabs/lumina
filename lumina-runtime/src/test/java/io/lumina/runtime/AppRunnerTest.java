package io.lumina.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumina.LuminaApp;
import io.lumina.ai.ChatClients;
import io.lumina.model.ComponentNode;
import io.lumina.model.ComponentTypes;
import io.lumina.ui.UploadedFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class AppRunnerTest {
    @Test
    void connectThenChatSubmitProducesAiEchoInTree() {
        LuminaApp app = ui -> {
            ui.title("Hello AI");
            var history = ui.state().computeIfAbsent("history", k -> new ArrayList<String[]>());
            for (String[] turn : history) {
                ui.user(turn[0]);
                ui.ai(turn[1]);
            }
            String prompt = ui.chatInput();
            if (prompt != null) {
                String reply = ChatClients.echo().prompt(prompt);
                history.add(new String[] {prompt, reply});
                ui.user(prompt);
                ui.ai(reply);
            }
        };
        SessionManager mgr = new SessionManager(app);
        SessionHandle session = mgr.create();

        RunResult initial = session.submit(Intent.connect()).join();
        String chatId = findType(initial.root(), ComponentTypes.CHAT_INPUT).id();
        RunResult after = session.submit(Intent.chatSubmit(chatId, "ping")).join();

        assertThat(flattenTypes(after.root()))
                .contains(ComponentTypes.USER_MESSAGE, ComponentTypes.AI_MESSAGE);
    }

    @Test
    void connectProducesFullSnapshotWithEmptyPatches() {
        LuminaApp app = ui -> ui.title("Hello");
        SessionHandle session = new SessionManager(app).create();

        RunResult result = session.submit(Intent.connect()).join();

        assertThat(result.fullSnapshot()).isTrue();
        assertThat(result.patches()).isEmpty();
        assertThat(result.error()).isNull();
    }

    @Test
    void subsequentIntentProducesIncrementalPatches() {
        LuminaApp app = ui -> {
            ui.title("Hello");
            if (ui.button("Add")) {
                ui.text("added");
            }
        };
        SessionHandle session = new SessionManager(app).create();
        RunResult initial = session.submit(Intent.connect()).join();
        String buttonId = findType(initial.root(), ComponentTypes.BUTTON).id();

        RunResult after = session.submit(Intent.click(buttonId)).join();

        assertThat(after.fullSnapshot()).isFalse();
        assertThat(after.patches()).isNotEmpty();
        assertThat(flattenTypes(after.root())).contains(ComponentTypes.TEXT);
    }

    @Test
    void userExceptionKeepsPreviousTreeAndSurfacesError() {
        LuminaApp app = ui -> {
            ui.title("Hello");
            if (ui.button("Boom")) {
                throw new IllegalStateException("boom");
            }
        };
        SessionHandle session = new SessionManager(app).create();
        RunResult initial = session.submit(Intent.connect()).join();
        String buttonId = findType(initial.root(), ComponentTypes.BUTTON).id();

        RunResult after = session.submit(Intent.click(buttonId)).join();

        assertThat(after.error()).isEqualTo("boom");
        assertThat(after.root()).isEqualTo(initial.root());
        assertThat(after.patches()).isEmpty();
    }

    @Test
    void failedInitialConnectReturnsErrorResult() {
        LuminaApp app = ui -> {
            throw new IllegalStateException("initial boom");
        };
        SessionHandle session = new SessionManager(app).create();

        RunResult result = session.submit(Intent.connect()).join();

        assertThat(result.error()).isEqualTo("initial boom");
        assertThat(result.root()).isNull();
        assertThat(result.patches()).isEmpty();
        assertThat(result.fullSnapshot()).isFalse();
    }

    @Test
    void intentsForSameSessionExecuteSerially() {
        LuminaApp app = ui -> {
            int[] count = ui.state().computeIfAbsent("count", k -> new int[] {0});
            if (ui.button("Inc")) {
                count[0]++;
            }
            ui.text("count:" + count[0]);
        };
        SessionHandle session = new SessionManager(app).create();
        RunResult initial = session.submit(Intent.connect()).join();
        String buttonId = findType(initial.root(), ComponentTypes.BUTTON).id();

        List<CompletableFuture<RunResult>> futures = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            futures.add(session.submit(Intent.click(buttonId)));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        ComponentNode textNode = findType(futures.get(futures.size() - 1).join().root(), ComponentTypes.TEXT);
        assertThat(textNode.props().get("content")).isEqualTo("count:20");
    }

    @Test
    void clickWithCompanionValuesAppliesInputsInSameRun() {
        LuminaApp app = ui -> {
            String name = ui.textInput("Name");
            if (ui.button("Greet") && !name.isBlank()) {
                ui.markdown("Hello, **" + name.trim() + "**");
            }
        };
        SessionHandle session = new SessionManager(app).create();
        RunResult initial = session.submit(Intent.connect()).join();
        String inputId = findType(initial.root(), ComponentTypes.TEXT_INPUT).id();
        String buttonId = findType(initial.root(), ComponentTypes.BUTTON).id();

        RunResult after = session.submit(new Intent(
                        "click",
                        buttonId,
                        Map.of("values", Map.of(inputId, "Ada"))))
                .join();

        assertThat(flattenTypes(after.root())).contains(ComponentTypes.MARKDOWN);
        assertThat(findType(after.root(), ComponentTypes.MARKDOWN).props().get("content"))
                .isEqualTo("Hello, **Ada**");
    }

    @Test
    void sessionsAreIsolatedPerHandle() {
        LuminaApp app = ui -> {
            String name = ui.textInput("Name");
            ui.text("hello " + name);
        };
        SessionManager mgr = new SessionManager(app);
        SessionHandle sessionA = mgr.create();
        SessionHandle sessionB = mgr.create();

        RunResult a = sessionA.submit(Intent.connect()).join();
        sessionB.submit(Intent.connect()).join();
        String inputIdA = findType(a.root(), ComponentTypes.TEXT_INPUT).id();
        sessionA.submit(Intent.input(inputIdA, "Ada")).join();

        RunResult bAfter = sessionB.submit(Intent.connect()).join();

        assertThat(findType(bAfter.root(), ComponentTypes.TEXT).props().get("content"))
                .isEqualTo("hello ");
    }

    @Test
    void fileUploadIntentMakesUploadedFileAvailableForOneRun() {
        LuminaApp app = ui -> ui.fileUpload("Attachment")
                .ifPresent(file -> ui.text(
                        file.fileName() + ":" + new String(file.bytes(), StandardCharsets.UTF_8)));
        SessionHandle session = new SessionManager(app).create();
        RunResult initial = session.submit(Intent.connect()).join();
        String uploadId = findType(initial.root(), ComponentTypes.FILE_UPLOAD).id();
        UploadedFile file =
                new UploadedFile("notes.txt", "text/plain", "hello".getBytes(StandardCharsets.UTF_8));

        RunResult uploaded = session.submit(new Intent("file_upload", uploadId, Map.of("file", file))).join();
        RunResult next = session.submit(Intent.connect()).join();

        assertThat(findType(uploaded.root(), ComponentTypes.TEXT).props().get("content"))
                .isEqualTo("notes.txt:hello");
        assertThat(flattenTypes(next.root())).doesNotContain(ComponentTypes.TEXT);
    }

    @Test
    void numericInputIntentAcceptsJsonNumberValues() {
        LuminaApp app = ui -> ui.text("retries:" + ui.numberInput("Retries", 1.0, 0.0, 5.0, 1.0));
        SessionHandle session = new SessionManager(app).create();
        RunResult initial = session.submit(Intent.connect()).join();
        String inputId = findType(initial.root(), ComponentTypes.NUMBER_INPUT).id();

        RunResult after = session.submit(new Intent("input", inputId, Map.of("value", 3))).join();

        assertThat(findType(after.root(), ComponentTypes.TEXT).props().get("content")).isEqualTo("retries:3.0");
    }

    @Test
    void downloadClickIsConsumedForOneRun() {
        LuminaApp app = ui -> {
            if (ui.downloadButton("Export", new byte[] {1}, "export.bin")) {
                ui.text("downloaded");
            }
        };
        SessionHandle session = new SessionManager(app).create();
        RunResult initial = session.submit(Intent.connect()).join();
        String downloadId = findType(initial.root(), ComponentTypes.DOWNLOAD_BUTTON).id();

        RunResult clicked = session.submit(Intent.click(downloadId)).join();
        RunResult next = session.submit(Intent.connect()).join();

        assertThat(flattenTypes(clicked.root())).contains(ComponentTypes.TEXT);
        assertThat(flattenTypes(next.root())).doesNotContain(ComponentTypes.TEXT);
    }

    private static ComponentNode findType(ComponentNode root, String type) {
        Deque<ComponentNode> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            ComponentNode node = stack.pop();
            if (type.equals(node.type())) {
                return node;
            }
            for (int i = node.children().size() - 1; i >= 0; i--) {
                stack.push(node.children().get(i));
            }
        }
        throw new AssertionError("No node of type " + type + " found in tree");
    }

    private static List<String> flattenTypes(ComponentNode root) {
        List<String> types = new ArrayList<>();
        flatten(root, types);
        return types;
    }

    private static void flatten(ComponentNode node, List<String> types) {
        types.add(node.type());
        node.children().forEach(child -> flatten(child, types));
    }
}
