package io.lumina.examples.ai;

import io.lumina.LuminaApp;
import io.lumina.ui.PageConfig;
import io.lumina.ui.Ui;

/** Standalone agent workbench demo. */
public final class AgentWorkbenchApp implements LuminaApp {
    @Override
    public void build(Ui ui) {
        ui.pageConfig(PageConfig.builder().title("Lumina Agent").build());
        AgentWorkbenchPages.build(ui);
    }
}
