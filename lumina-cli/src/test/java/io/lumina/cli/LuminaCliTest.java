package io.lumina.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringWriter;
import org.junit.jupiter.api.Test;

class LuminaCliTest {
    @Test
    void helpExitsZero() {
        int code = LuminaCli.run(new String[] {"--help"}, new StringWriter());
        assertThat(code).isEqualTo(0);
    }
}
