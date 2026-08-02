package io.lumina.examples.agent;

import io.lumina.LuminaApp;
import io.lumina.examples.ai.AgentWorkbenchPages;
import io.lumina.ui.PageConfig;
import io.lumina.ui.Ui;

/**
 * Human-in-the-loop agent interface (delegates to the AI workbench pages).
 */
public final class AgentDemoApp implements LuminaApp {
    @Override
    public void build(Ui ui) {
        ui.pageConfig(PageConfig.builder().title("Lumina Agent").build());
        AgentWorkbenchPages.build(ui);
    }
}
