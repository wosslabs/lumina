package io.lumina.examples.layout;

import io.lumina.LuminaApp;
import io.lumina.ui.Ui;

/**
 * Demonstrates nested layout primitives: sidebar, columns, container, and expander.
 */
public final class LayoutDemoApp implements LuminaApp {

    /**
     * Creates a layout demo application.
     */
    public LayoutDemoApp() {}

    @Override
    public void build(Ui ui) {
        ui.sidebar(sidebar -> {
            sidebar.brand(brand -> brand.markdown("## Layout demo"));
            sidebar.footer(footer -> {
                if (footer.button("Refresh")) {
                    /* rerun */
                }
            });
        });
        ui.columns(2, cols -> {
            cols[0].markdown("### Left");
            cols[1].container(inner -> inner.text("Nested content"));
        });
        ui.expander("Details", body -> body.code("java", "System.out.println();"));
    }
}
