package io.lumina.ai;

import java.util.ArrayList;
import java.util.List;
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
        List<String> chunks = new ArrayList<>();
        ChatClients.echo().stream("one two three").forEach(chunks::add);
        assertThat(chunks.size()).isGreaterThan(1);
    }

    @Test
    void streamEmitsWordAlignedChunksForHelloWorld() {
        List<String> chunks = new ArrayList<>();
        ChatClients.echo().stream("hello world").forEach(chunks::add);
        assertThat(chunks).containsExactly("Echo: ", "hello ", "world");
    }

    @Test
    void defaultStreamYieldsSingleChunkEqualToPrompt() {
        ChatClient client = input -> "fixed reply";
        List<String> chunks = new ArrayList<>();
        client.stream("any input").forEach(chunks::add);
        assertThat(chunks).containsExactly(client.prompt("any input"));
    }
}
