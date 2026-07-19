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

    /**
     * Registers a Spring AI-backed Lumina {@link io.lumina.ai.ChatClient} built from the
     * application's {@link ChatModel}.
     *
     * <p>Requires exactly one candidate {@link ChatModel} bean: if the context defines several
     * without a {@code @Primary} (or otherwise qualified) one, injection fails with
     * {@code NoUniqueBeanDefinitionException}, as with any single-typed Spring injection point.
     *
     * @param chatModel the Spring AI chat model to adapt; must resolve to a unique bean
     * @return a Lumina chat client streaming replies through the model
     */
    @Bean
    @ConditionalOnBean(ChatModel.class)
    @ConditionalOnMissingBean(io.lumina.ai.ChatClient.class)
    public io.lumina.ai.ChatClient luminaSpringAiChatClient(ChatModel chatModel) {
        return new SpringAiChatClient(ChatClient.create(chatModel));
    }
}
