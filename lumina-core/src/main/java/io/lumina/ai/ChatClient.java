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
}
