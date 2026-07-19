package io.lumina.ai;

import java.util.List;
import java.util.Objects;

/**
 * A blocking, forward-only source of text chunks produced by a {@link ChatClient}.
 * Iteration blocks until the next chunk is available or the stream ends.
 */
public interface TokenStream extends Iterable<String> {

    /**
     * Wraps a complete string as a single-chunk stream, for non-streaming clients.
     *
     * @param whole full text; must not be null
     * @return a stream yielding {@code whole} as one chunk
     */
    static TokenStream of(String whole) {
        Objects.requireNonNull(whole, "whole");
        return fromIterable(List.of(whole));
    }

    /**
     * Wraps an existing iterable of chunks as a token stream.
     *
     * @param chunks chunk source; must not be null
     * @return a stream yielding {@code chunks} in order
     */
    static TokenStream fromIterable(Iterable<String> chunks) {
        Objects.requireNonNull(chunks, "chunks");
        return chunks::iterator;
    }
}
