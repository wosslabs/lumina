package io.lumina.examples.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DemoCorpusTest {
    @Test
    void retrieveRanksArchitectureForArchitectureQuery() {
        List<Map<String, Object>> hits = DemoCorpus.retrieve("architecture websocket", 3);
        assertThat(hits).isNotEmpty();
        assertThat(hits.get(0).get("title").toString().toLowerCase()).contains("architecture");
    }

    @Test
    void mcpCatalogContainsSearchTool() {
        assertThat(DemoMcpCatalog.require("docs.search").description()).isNotBlank();
        assertThat(DemoMcpCatalog.require("math.add").handler().apply("2 3")).isEqualTo("5");
    }
}
