package io.lumina.springai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration registering a {@link io.lumina.ai.ChatClient} backed by Spring AI when a
 * Spring AI {@link ChatModel} bean is available and no Lumina chat client has been configured.
 *
 * <p>Spring AI's own auto-configuration registers a {@link ChatModel} bean (and a {@code
 * ChatClient.Builder} bean built from it) but never a {@link ChatClient} bean directly, so this
 * configuration builds the fluent {@link ChatClient} itself from the {@link ChatModel}.
 */
@AutoConfiguration
@ConditionalOnClass(ChatClient.class)
public class LuminaSpringAiAutoConfiguration {
    /**
     * Creates the Spring AI Lumina auto-configuration.
     */
    public LuminaSpringAiAutoConfiguration() {}

    @Bean
    @ConditionalOnBean(ChatModel.class)
    @ConditionalOnMissingBean(io.lumina.ai.ChatClient.class)
    public io.lumina.ai.ChatClient luminaSpringAiChatClient(ChatModel chatModel) {
        return new SpringAiChatClient(ChatClient.create(chatModel));
    }
}
