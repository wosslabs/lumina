package io.lumina.ai;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TokenStreamTest {
    @Test
    void ofYieldsSingleChunk() {
        List<String> chunks = new ArrayList<>();
        TokenStream.of("hello world").forEach(chunks::add);
        assertThat(chunks).containsExactly("hello world");
    }

    @Test
    void fromIterablePreservesChunks() {
        List<String> chunks = new ArrayList<>();
        TokenStream.fromIterable(List.of("a", "b", "c")).forEach(chunks::add);
        assertThat(chunks).containsExactly("a", "b", "c");
    }

    @Test
    void chunksConcatenateToWhole() {
        StringBuilder sb = new StringBuilder();
        TokenStream.fromIterable(List.of("Hel", "lo")).forEach(sb::append);
        assertThat(sb.toString()).isEqualTo("Hello");
    }
}
