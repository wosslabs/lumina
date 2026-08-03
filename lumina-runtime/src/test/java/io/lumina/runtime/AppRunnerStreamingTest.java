package io.lumina.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumina.LuminaApp;
import io.lumina.ai.ChatClients;
import io.lumina.ai.TokenStream;
import io.lumina.model.ComponentNode;
import io.lumina.model.ComponentTypes;
import io.lumina.session.internal.SessionState;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;

class AppRunnerStreamingTest {

    @Test
    void streamingRunFlushesInterimThenEmitsFramesThenSuppressesFinalUpdate() {
        LuminaApp app = ui -> {
            String p = ui.chatInput();
            if (p != null) {
                ui.user(p);
                ui.ai(ChatClients.echo().stream(p));
            }
        };
        SessionState session = new SessionState();
        AppRunner runner = new AppRunner();

        List<RunResult> connectInterims = new ArrayList<>();
        List<String> connectFrames = new ArrayList<>();
        RunResult connect = runner.run(app, session, Intent.connect(),
                new CapturingSink(connectInterims, connectFrames));
        assertThat(connect.fullSnapshot()).isTrue();

        String chatId = connect.root().children().stream()
                .filter(n -> n.type().equals(ComponentTypes.CHAT_INPUT))
                .findFirst().orElseThrow().id();

        List<RunResult> interims = new ArrayList<>();
        List<String> frames = new ArrayList<>();
        RunResult result = runner.run(app, session, Intent.chatSubmit(chatId, "hi there"),
                new CapturingSink(interims, frames));

        assertThat(interims).isNotEmpty();
        assertThat(frames).anyMatch(f -> f.contains("\"op\":\"start\""));
        assertThat(frames).anyMatch(f -> f.contains("\"op\":\"append\""));
        assertThat(frames).anyMatch(f -> f.contains("\"op\":\"end\""));

        String appended = frames.stream()
                .filter(f -> f.contains("\"op\":\"append\""))
                .map(AppRunnerStreamingTest::extractAppendedText)
                .reduce("", String::concat);
        assertThat(appended).isEqualTo(ChatClients.echo().prompt("hi there"));

        // Streamed ai_message content is suppressed; the remaining patch clears composer busy.
        assertThat(result.patches()).hasSize(1);
        assertThat(result.patches().getFirst().op()).isEqualTo("UPDATE_PROPS");
        assertThat(result.patches().getFirst().props()).doesNotContainKey("busy");
        ComponentNode chatInput = result.root().children().stream()
                .filter(n -> n.type().equals(ComponentTypes.CHAT_INPUT))
                .findFirst()
                .orElseThrow();
        assertThat(chatInput.props()).doesNotContainKey("busy");
    }

    @Test
    void aiStreamThrowingMidStreamStillEmitsStreamEndAndSurfacesError() {
        TokenStream throwing = () -> new Iterator<>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public String next() {
                if (index++ == 0) {
                    return "first";
                }
                throw new IllegalStateException("stream boom");
            }
        };
        LuminaApp app = ui -> ui.ai(throwing);
        SessionState session = new SessionState();
        AppRunner runner = new AppRunner();
        List<RunResult> interims = new ArrayList<>();
        List<String> frames = new ArrayList<>();

        RunResult result = runner.run(app, session, Intent.connect(), new CapturingSink(interims, frames));

        assertThat(result.hasError()).isTrue();
        assertThat(frames).anyMatch(f -> f.contains("\"op\":\"end\""));
    }

    private static String extractAppendedText(String frame) {
        String marker = "\"text\":\"";
        int start = frame.indexOf(marker) + marker.length();
        int end = frame.indexOf('"', start);
        return frame.substring(start, end);
    }

    private record CapturingSink(List<RunResult> interims, List<String> frames) implements RunSink {
        @Override
        public void deliverInterim(RunResult interim) {
            interims.add(interim);
        }

        @Override
        public void sendFrame(String json) {
            frames.add(json);
        }
    }
}
