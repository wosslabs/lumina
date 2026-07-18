package io.lumina.spring;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumina.LuminaApp;
import io.lumina.web.LuminaServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;

@SpringBootTest(
        classes = LuminaAutoConfigurationTest.TestApp.class,
        properties = {"lumina.port=0", "spring.config.import="})
@ImportAutoConfiguration(LuminaAutoConfiguration.class)
class LuminaAutoConfigurationTest {
    @Test
    void startsServer(@Autowired LuminaServer server) {
        assertThat(server.port()).isPositive();
    }

    @Test
    void defaultsPortTo8080() {
        assertThat(new LuminaProperties().getPort()).isEqualTo(8080);
    }

    @Test
    void stopsServerThroughLifecycle() {
        LuminaProperties properties = new LuminaProperties();
        properties.setPort(0);
        LuminaAutoConfiguration.LuminaServerLifecycle lifecycle =
                new LuminaAutoConfiguration.LuminaServerLifecycle(
                        ui -> ui.title("lifecycle"), properties);

        lifecycle.start();
        assertThat(lifecycle.isRunning()).isTrue();

        lifecycle.stop();
        assertThat(lifecycle.isRunning()).isFalse();
    }

    @SpringBootConfiguration
    static class TestApp {
        @Bean
        LuminaApp app() {
            return ui -> ui.title("boot");
        }
    }
}
