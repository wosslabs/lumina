package io.lumina.examples.ai;

import io.lumina.LuminaApp;
import io.lumina.ui.PageConfig;
import io.lumina.ui.Ui;

/** Standalone MCP tool console demo. */
public final class McpConsoleApp implements LuminaApp {
    @Override
    public void build(Ui ui) {
        ui.pageConfig(PageConfig.builder().title("Lumina MCP").build());
        McpConsolePages.build(ui);
    }
}
