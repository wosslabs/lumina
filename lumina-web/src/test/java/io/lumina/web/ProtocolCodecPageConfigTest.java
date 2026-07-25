package io.lumina.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumina.model.ComponentNode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProtocolCodecPageConfigTest {

    @Test
    void snapshotIncludesRootPageConfigProps() {
        Map<String, Object> props = Map.of(
                "pageTitle", "Dash",
                "layout", "wide",
                "sidebarState", "expanded");
        ComponentNode root = new ComponentNode("root", "root", props, List.of());
        String json = ProtocolCodec.toSnapshotJson(root);

        assertThat(json).contains("\"pageTitle\":\"Dash\"");
        assertThat(json).contains("\"layout\":\"wide\"");
    }
}
