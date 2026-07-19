package io.lumina.ai;

/**
 * SPI for language-model style completions.
 */
public interface ChatClient {
    /**
     * Produce a completion for the given prompt.
     *
     * @param input user prompt; never null
     * @return model reply; never null
     */
    String prompt(String input);

    /**
     * Streams the completion as chunks. The default yields the whole {@link #prompt(String)}
     * result as a single chunk, preserving source and binary compatibility for existing clients.
     *
     * @param input user prompt; never null
     * @return token stream of the reply; never null
     */
    default TokenStream stream(String input) {
        return TokenStream.of(prompt(input));
    }
}
