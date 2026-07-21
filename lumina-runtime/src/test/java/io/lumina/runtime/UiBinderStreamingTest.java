package io.lumina.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumina.ai.TokenStream;
import io.lumina.model.ComponentNode;
import io.lumina.model.ComponentTypes;
import io.lumina.session.internal.SessionState;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class UiBinderStreamingTest {

    private static final class RecordingBridge implements StreamBridge {
        final List<String> events = new ArrayList<>();
        int flushes = 0;
        ComponentNode lastInterimRoot;

        @Override public void flushBefore(ComponentNode interimRoot) {
            flushes++;
            lastInterimRoot = interimRoot;
        }
        @Override public void streamStart(String nodeId) { events.add("start:" + nodeId); }
        @Override public void streamAppend(String nodeId, String text) { events.add("append:" + text); }
        @Override public void streamEnd(String nodeId) { events.add("end:" + nodeId); }
    }

    @Test
    void aiStreamEmitsFlushStartAppendsEndAndReturnsText() {
        SessionState session = new SessionState();
        RecordingBridge bridge = new RecordingBridge();
        UiBinder ui = new UiBinder(session, bridge);

        String result = ui.ai(TokenStream.fromIterable(List.of("Hel", "lo")));

        assertThat(result).isEqualTo("Hello");
        assertThat(bridge.flushes).isEqualTo(1);
        assertThat(bridge.events).containsSubsequence("append:Hel", "append:lo");
        assertThat(bridge.events.get(0)).startsWith("start:");
        assertThat(bridge.events.get(bridge.events.size() - 1)).startsWith("end:");
    }

    @Test
    void streamedAiNodeCarriesFullContentAndIsMarked() {
        SessionState session = new SessionState();
        RecordingBridge bridge = new RecordingBridge();
        UiBinder ui = new UiBinder(session, bridge);

        ui.ai(TokenStream.fromIterable(List.of("Hel", "lo")));
        ComponentNode root = ui.buildRoot();

        ComponentNode ai = root.children().stream()
                .filter(n -> n.type().equals(ComponentTypes.AI_MESSAGE)).findFirst().orElseThrow();
        assertThat(ai.props().get("content")).isEqualTo("Hello");
        assertThat(ui.streamedNodeIds()).contains(ai.id());
    }

    @Test
    void nonStreamingBinderStillWorks() {
        UiBinder ui = new UiBinder(new SessionState());
        ui.ai("done");
        assertThat(ui.buildRoot().children()).hasSize(1);
    }

    @Test
    void streamingInsideContainerFlushesFullRootSnapshot() {
        SessionState session = new SessionState();
        RecordingBridge bridge = new RecordingBridge();
        UiBinder ui = new UiBinder(session, bridge);

        ui.title("Outside");
        ui.container(box -> box.ai(TokenStream.fromIterable(List.of("Hi"))));

        assertThat(bridge.flushes).isEqualTo(1);
        assertThat(bridge.lastInterimRoot.children()).hasSize(2);
        assertThat(bridge.lastInterimRoot.children().get(0).type()).isEqualTo(ComponentTypes.TITLE);
        assertThat(bridge.lastInterimRoot.children().get(1).type()).isEqualTo(ComponentTypes.CONTAINER);
        assertThat(bridge.lastInterimRoot.children().get(1).children()).hasSize(1);
        assertThat(bridge.lastInterimRoot.children().get(1).children().getFirst().type())
                .isEqualTo(ComponentTypes.AI_MESSAGE);
    }
}
