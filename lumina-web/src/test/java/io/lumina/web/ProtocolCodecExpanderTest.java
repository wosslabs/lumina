package io.lumina.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumina.runtime.Intent;
import org.junit.jupiter.api.Test;

class ProtocolCodecExpanderTest {
    @Test
    void parsesExpanderToggleIntent() {
        Intent intent = ProtocolCodec.parseIntent(
                "{\"type\":\"intent\",\"name\":\"expander_toggle\",\"targetId\":\"auto:/expander#0\"}");
        assertThat(intent.name()).isEqualTo("expander_toggle");
        assertThat(intent.targetId()).isEqualTo("auto:/expander#0");
        assertThat(intent.payload()).isEmpty();
    }
}
