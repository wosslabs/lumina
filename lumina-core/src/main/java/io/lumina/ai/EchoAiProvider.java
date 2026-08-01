package io.lumina.ai;

/**
 * Offline provider that echoes prompts for local development.
 */
public final class EchoAiProvider implements AiProvider {
    private final EchoChatClient client = new EchoChatClient();

    @Override
    public TokenStream stream(String prompt) {
        return client.stream(prompt);
    }
}
