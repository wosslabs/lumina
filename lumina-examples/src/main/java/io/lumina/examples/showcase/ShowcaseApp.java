package io.lumina.examples.showcase;

import io.lumina.LuminaApp;
import io.lumina.ui.PageConfig;
import io.lumina.ui.PageLayout;
import io.lumina.ui.SidebarState;
import io.lumina.ui.Ui;

/**
 * Hero demo showcasing P1.5 UX: page config, app shell, sidebar rail, and styled widgets.
 */
public final class ShowcaseApp implements LuminaApp {

    /**
     * Creates the showcase application.
     */
    public ShowcaseApp() {}

    @Override
    public void build(Ui ui) {
        ui.pageConfig(PageConfig.builder()
                .title("Lumina Showcase")
                .layout(PageLayout.WIDE)
                .sidebar(SidebarState.EXPANDED)
                .build());

        ui.sidebar(nav -> {
            nav.markdown("## Navigation");
            nav.button("Home");
            nav.button("Settings");
        });

        ui.title("Dashboard");

        ui.columns(3, cols -> {
            cols[0].container(box -> {
                box.markdown("### Users");
                box.markdown("**1,284**");
            });
            cols[1].container(box -> {
                box.markdown("### Revenue");
                box.markdown("**$48.2k**");
            });
            cols[2].container(box -> {
                box.markdown("### Goal");
                box.progress(0.72);
            });
        });

        ui.expander("Advanced", body -> body.code("java", "ui.pageConfig(...);"));

        ui.columns(2, cols -> {
            cols[0].textInput("Filter");
            cols[1].button("Apply");
        });
    }
}
