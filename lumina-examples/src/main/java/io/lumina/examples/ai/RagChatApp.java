package io.lumina.examples.ai;

import io.lumina.LuminaApp;
import io.lumina.ui.PageConfig;
import io.lumina.ui.Ui;

/** Standalone RAG chat demo. */
public final class RagChatApp implements LuminaApp {
    @Override
    public void build(Ui ui) {
        ui.pageConfig(PageConfig.builder().title("Lumina RAG").build());
        RagChatPages.build(ui);
    }
}
