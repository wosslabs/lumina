package io.lumina.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumina.ai.TokenStream;
import io.lumina.model.ComponentNode;
import io.lumina.model.ComponentTypes;
import io.lumina.session.internal.SessionState;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class UiBinderChatBusyTest {

    private static final class RecordingBridge implements StreamBridge {
        final List<ComponentNode> interimRoots = new ArrayList<>();

        @Override
        public void flushBefore(ComponentNode interimRoot) {
            interimRoots.add(interimRoot);
        }

        @Override
        public void streamStart(String nodeId) {}

        @Override
        public void streamAppend(String nodeId, String text) {}

        @Override
        public void streamEnd(String nodeId) {}
    }

    @Test
    void aiStreamMarksComposerBusyOnInterimFlush() {
        RecordingBridge bridge = new RecordingBridge();
        UiBinder ui = new UiBinder(new SessionState(), bridge);
        List<Boolean> busyDuringChunks = new ArrayList<>();

        ui.chatShell(shell -> {
            shell.composer(c -> c.chatInput());
            shell.transcript(t -> t.ai(new TokenStream() {
                @Override
                public Iterator<String> iterator() {
                    return new Iterator<>() {
                        private int index;

                        @Override
                        public boolean hasNext() {
                            return index < 2;
                        }

                        @Override
                        public String next() {
                            if (!hasNext()) {
                                throw new NoSuchElementException();
                            }
                            if (index == 0) {
                                busyDuringChunks.add(composerBusy(bridge.interimRoots.getLast()));
                            }
                            return index++ == 0 ? "a" : "b";
                        }
                    };
                }
            }));
        });

        assertThat(bridge.interimRoots).isNotEmpty();
        assertThat(composerBusy(bridge.interimRoots.getFirst())).isTrue();
        assertThat(busyDuringChunks).containsExactly(true);
        assertThat(composerBusy(ui.buildRoot())).isFalse();
    }

    @Test
    void bareChatInputMarkedBusyDuringStream() {
        RecordingBridge bridge = new RecordingBridge();
        UiBinder ui = new UiBinder(new SessionState(), bridge);
        ui.chatInput();
        ui.ai(TokenStream.fromIterable(List.of("x")));
        assertThat(chatInputBusy(bridge.interimRoots.getFirst())).isTrue();
        assertThat(chatInputBusy(ui.buildRoot())).isFalse();
    }

    private static boolean composerBusy(ComponentNode root) {
        return Boolean.TRUE.equals(requireType(root, ComponentTypes.CHAT_COMPOSER).props().get("busy"));
    }

    private static boolean chatInputBusy(ComponentNode root) {
        return Boolean.TRUE.equals(requireType(root, ComponentTypes.CHAT_INPUT).props().get("busy"));
    }

    private static ComponentNode findType(ComponentNode node, String type) {
        if (type.equals(node.type())) {
            return node;
        }
        for (ComponentNode child : node.children()) {
            ComponentNode found = findType(child, type);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static ComponentNode requireType(ComponentNode root, String type) {
        ComponentNode found = findType(root, type);
        assertThat(found).as(type).isNotNull();
        return found;
    }
}
