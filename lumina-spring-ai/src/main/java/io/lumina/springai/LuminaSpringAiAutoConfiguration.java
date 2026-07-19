package io.lumina.springai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration registering a {@link io.lumina.ai.ChatClient} backed by Spring AI when a
 * Spring AI {@link ChatClient} bean is available and no Lumina chat client has been configured.
 */
@AutoConfiguration
@ConditionalOnClass(ChatClient.class)
public class LuminaSpringAiAutoConfiguration {
    /**
     * Creates the Spring AI Lumina auto-configuration.
     */
    public LuminaSpringAiAutoConfiguration() {}

    @Bean
    @ConditionalOnBean(ChatClient.class)
    @ConditionalOnMissingBean(io.lumina.ai.ChatClient.class)
    public io.lumina.ai.ChatClient luminaSpringAiChatClient(ChatClient chatClient) {
        return new SpringAiChatClient(chatClient);
    }
}
