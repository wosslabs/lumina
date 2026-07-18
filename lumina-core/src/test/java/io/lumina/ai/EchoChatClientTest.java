package io.lumina.ai;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EchoChatClientTest {
    @Test
    void promptEchoesWithPrefix() {
        ChatClient client = ChatClients.echo();
        assertThat(client.prompt("hello")).isEqualTo("Echo: hello");
    }
}
