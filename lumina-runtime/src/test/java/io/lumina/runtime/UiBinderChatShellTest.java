package io.lumina.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumina.model.ComponentNode;
import io.lumina.model.ComponentTypes;
import io.lumina.session.internal.SessionState;
import io.lumina.ui.ChatShellOptions;
import io.lumina.ui.PageConfig;
import io.lumina.ui.PageLayout;
import org.junit.jupiter.api.Test;

class UiBinderChatShellTest {

    @Test
    void chatShellBuildsHeaderComposerTranscript() {
        UiBinder ui = new UiBinder(new SessionState());
        ui.pageConfig(PageConfig.builder().title("T").layout(PageLayout.CHAT).build());
        ui.chatShell(shell -> {
            shell.header(h -> h.title("Chat"));
            shell.composer(c -> c.chatInput());
            shell.transcript(t -> {
                t.user("hi");
                t.ai("hello");
            });
        });

        ComponentNode root = ui.buildRoot();
        assertThat(root.props().get("layout")).isEqualTo("chat");
        ComponentNode shell = root.children().stream()
                .filter(n -> ComponentTypes.CHAT_SHELL.equals(n.type()))
                .findFirst()
                .orElseThrow();
        assertThat(shell.props().get("newestFirst")).isEqualTo(true);
        assertThat(shell.children().stream().map(ComponentNode::type).toList())
                .containsExactly(
                        ComponentTypes.CHAT_HEADER,
                        ComponentTypes.CHAT_COMPOSER,
                        ComponentTypes.CHAT_TRANSCRIPT);
    }

    @Test
    void chatShellOptionsCanDisableNewestFirst() {
        UiBinder ui = new UiBinder(new SessionState());
        ui.chatShell(new ChatShellOptions(false), shell -> shell.composer(c -> c.chatInput()));
        ComponentNode shell = ui.buildRoot().children().getFirst();
        assertThat(shell.props().get("newestFirst")).isEqualTo(false);
    }
}
