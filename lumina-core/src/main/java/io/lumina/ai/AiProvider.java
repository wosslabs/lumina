package io.lumina.ai;

/**
 * Provider-neutral source of streamed AI response tokens.
 */
@FunctionalInterface
public interface AiProvider {
    /**
     * Streams a response for the supplied prompt.
     *
     * @param prompt user prompt
     * @return response token stream
     */
    TokenStream stream(String prompt);
}
