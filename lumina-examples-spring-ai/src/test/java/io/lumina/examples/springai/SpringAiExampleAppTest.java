package io.lumina.examples.springai;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumina.LuminaApp;
import io.lumina.ai.ChatClient;
import io.lumina.springai.SpringAiChatClient;
import io.lumina.web.LuminaServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        classes = SpringAiExampleApp.class,
        properties = {"lumina.port=0", "server.port=0", "spring.config.import="})
class SpringAiExampleAppTest {
    @Test
    void contextLoadsLuminaApp(@Autowired LuminaApp luminaApp) {
        assertThat(luminaApp).isNotNull();
    }

    @Test
    void startsLuminaServer(@Autowired LuminaServer server) {
        assertThat(server.port()).isPositive();
    }

    @Test
    void providesChatClient(@Autowired ChatClient chatClient) {
        assertThat(chatClient).isNotNull();
    }

    @Test
    void usesEchoWhenNoApiKey(@Autowired ChatClient chatClient) {
        assertThat(chatClient).isNotInstanceOf(SpringAiChatClient.class);
    }
}
