package io.lumina.examples.spring;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumina.LuminaApp;
import io.lumina.web.LuminaServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        classes = SpringBootExampleApp.class,
        properties = {"lumina.port=0", "server.port=0", "spring.config.import="})
class SpringBootExampleAppTest {
    @Test
    void contextLoadsLuminaApp(@Autowired LuminaApp luminaApp) {
        assertThat(luminaApp).isNotNull();
    }

    @Test
    void startsLuminaServer(@Autowired LuminaServer server) {
        assertThat(server.port()).isPositive();
    }
}
