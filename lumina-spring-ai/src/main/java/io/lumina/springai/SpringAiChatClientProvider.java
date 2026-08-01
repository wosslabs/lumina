package io.lumina.springai;

import io.lumina.ai.AiProvider;
import io.lumina.ai.TokenStream;
import java.util.Objects;
import org.springframework.ai.chat.client.ChatClient;

/**
 * {@link AiProvider} adapter for an application-managed Spring AI {@link ChatClient}.
 */
public final class SpringAiChatClientProvider implements AiProvider {
    private final SpringAiChatClient delegate;

    public SpringAiChatClientProvider(ChatClient chatClient) {
        this.delegate = new SpringAiChatClient(Objects.requireNonNull(chatClient, "chatClient"));
    }

    @Override
    public TokenStream stream(String prompt) {
        return delegate.stream(prompt);
    }
}
