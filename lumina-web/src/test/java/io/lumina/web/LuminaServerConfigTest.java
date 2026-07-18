package io.lumina.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LuminaServerConfigTest {
    @Test
    void defaultsBindToLoopbackOnly() {
        LuminaServerConfig config = LuminaServerConfig.defaults();

        assertThat(config.host()).isEqualTo("127.0.0.1");
        assertThat(config.port()).isEqualTo(8080);
    }

    @Test
    void defaultsCapSessionsAndSetIdleTimeout() {
        LuminaServerConfig config = LuminaServerConfig.defaults();

        assertThat(config.maxSessions()).isEqualTo(LuminaServerConfig.DEFAULT_MAX_SESSIONS);
        assertThat(config.idleTimeout()).isEqualTo(LuminaServerConfig.DEFAULT_IDLE_TIMEOUT);
        assertThat(config.allowedOrigins()).isEmpty();
    }

    @Test
    void buildersOverrideEveryField() {
        LuminaServerConfig config = LuminaServerConfig.builder()
                .host("0.0.0.0")
                .port(9090)
                .maxSessions(5)
                .idleTimeout(Duration.ofMinutes(5))
                .allowedOrigins(Set.of("https://example.com"))
                .build();

        assertThat(config.host()).isEqualTo("0.0.0.0");
        assertThat(config.port()).isEqualTo(9090);
        assertThat(config.maxSessions()).isEqualTo(5);
        assertThat(config.idleTimeout()).isEqualTo(Duration.ofMinutes(5));
        assertThat(config.allowedOrigins()).containsExactly("https://example.com");
    }

    @Test
    void rejectsNonPositiveMaxSessions() {
        assertThatThrownBy(() -> LuminaServerConfig.builder().maxSessions(0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullIdleTimeout() {
        assertThatThrownBy(() -> LuminaServerConfig.builder().idleTimeout(null))
                .isInstanceOf(NullPointerException.class);
    }
}
