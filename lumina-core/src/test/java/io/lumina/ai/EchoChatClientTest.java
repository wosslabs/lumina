package io.lumina.ai;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EchoChatClientTest {
    @Test
    void promptEchoesWithPrefix() {
        ChatClient client = ChatClients.echo();
        assertThat(client.prompt("hello")).isEqualTo("Echo: hello");
    }

    @Test
    void streamChunksConcatenateToPromptResult() {
        ChatClient client = ChatClients.echo();
        StringBuilder sb = new StringBuilder();
        client.stream("hello world").forEach(sb::append);
        assertThat(sb.toString()).isEqualTo(client.prompt("hello world"));
    }

    @Test
    void streamEmitsMultipleChunksForMultiWord() {
        java.util.List<String> chunks = new java.util.ArrayList<>();
        ChatClients.echo().stream("one two three").forEach(chunks::add);
        assertThat(chunks.size()).isGreaterThan(1);
    }
}
