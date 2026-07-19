package io.lumina.springai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumina.LuminaException;
import io.lumina.ai.TokenStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

class SpringAiChatClientTest {
    @Test
    void bridgesFluxChunksInOrder() {
        TokenStream stream = SpringAiChatClient.toTokenStream(Flux.just("Hel", "lo", "!"));
        List<String> chunks = new ArrayList<>();
        stream.forEach(chunks::add);
        assertThat(chunks).containsExactly("Hel", "lo", "!");
    }

    @Test
    void propagatesEmptyFluxAsNoChunks() {
        List<String> chunks = new ArrayList<>();
        SpringAiChatClient.toTokenStream(Flux.empty()).forEach(chunks::add);
        assertThat(chunks).isEmpty();
    }

    @Test
    void wrapsUpstreamErrorAsLuminaException() {
        TokenStream stream = SpringAiChatClient.toTokenStream(Flux.error(new RuntimeException("boom")));

        assertThatThrownBy(() -> stream.forEach(chunk -> {}))
                .isInstanceOf(LuminaException.class)
                .hasCauseInstanceOf(RuntimeException.class)
                .cause()
                .hasMessage("boom");
    }
}
