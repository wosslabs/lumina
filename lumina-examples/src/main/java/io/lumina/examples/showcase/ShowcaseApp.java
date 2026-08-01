package io.lumina.examples.showcase;

import io.lumina.LuminaApp;
import io.lumina.ui.PageConfig;
import io.lumina.ui.PageLayout;
import io.lumina.ui.SidebarState;
import io.lumina.ui.Ui;

/**
 * Interactive hero demo: Streamlit-style reruns, multi-page routing, session state, and layout.
 */
public final class ShowcaseApp implements LuminaApp {

    /**
     * Creates the showcase application.
     */
    public ShowcaseApp() {}

    @Override
    public void build(Ui ui) {
        ui.pageConfig(PageConfig.builder()
                .title("Lumina")
                .layout(PageLayout.WIDE)
                .sidebar(SidebarState.EXPANDED)
                .build());

        var state = ui.state();
        int countValue = state.computeIfAbsent("count", key -> 0);
        double progressValue = state.computeIfAbsent("progress", key -> 0.0);

        ui.sidebar(sidebar -> {
            sidebar.brand(brand -> {
                brand.markdown("## Lumina");
                brand.text("Pure Java · server-driven · Streamlit-style reruns.");
            });
            sidebar.nav(nav -> {
                nav.page("Home", "/");
                nav.page("Widgets", "/widgets");
                nav.page("AI extras", "/ai");
                nav.page("About", "/about");
            });
            sidebar.footer(footer -> {
                footer.markdown("### Session");
                if (footer.button("Reset demo")) {
                    state.remove("count");
                    state.remove("progress");
                    state.remove("greeting");
                }
            });
        });

        switch (ui.path()) {
            case "/about" -> buildAbout(ui);
            case "/widgets" -> buildWidgets(ui);
            case "/ai" -> buildAi(ui);
            default -> buildHome(ui, state, countValue, progressValue);
        }
    }

    private void buildHome(Ui ui, io.lumina.state.StateStore state, int countValue, double progressValue) {
        ui.header(header -> header.title("Showcase / Home"));
        ui.title("Streamlit-style apps in Java");
        ui.markdown(
                "Lumina is an open-source framework for **interactive, server-driven UIs**. "
                        + "Declare widgets in Java, rerun on every interaction, keep state on the server.");

        ui.markdown("### Try the rerun loop");
        ui.text("Click a button — the server reruns `build()` and patches the UI.");

        ui.columns(2, cols -> {
            cols[0].container(box -> {
                box.markdown("#### Counter");
                if (box.button("Increment")) {
                    state.set("count", countValue + 1);
                }
                int displayed = state.get("count") instanceof Integer value ? value : 0;
                box.markdown("Current count: **" + displayed + "**");
            });
            cols[1].container(box -> {
                box.markdown("#### Progress");
                if (box.button("Advance")) {
                    state.set("progress", Math.min(1.0, progressValue + 0.15));
                }
                double displayed = state.get("progress") instanceof Number value ? value.doubleValue() : 0.0;
                box.progress(displayed);
                box.text(String.format("%.0f%% complete", displayed * 100));
            });
        });

        ui.markdown("### Inputs");
        String name = ui.textInput("Your name");
        if (ui.button("Say hello")) {
            state.set("greeting", name.isBlank() ? "stranger" : name.trim());
        }
        String greeting = state.get("greeting");
        if (greeting != null) {
            ui.markdown("Hello, **" + greeting + "**!");
        }
    }

    private void buildAbout(Ui ui) {
        ui.header(header -> header.title("Showcase / About"));
        ui.title("About Lumina");
        ui.markdown(
                "Lumina targets Java teams who want **Streamlit-like productivity** without Python or "
                        + "author-written HTML/CSS/JS.");
        ui.markdown("### Routing");
        ui.text("You clicked About in the sidebar. The app called ui.navigate(\"/about\") and reran on the server.");
        ui.markdown("Current path: **" + ui.path() + "**");
        ui.expander("Example", body -> body.code(
                "java",
                """
                sidebar.nav(nav -> nav.page("About", "/about"));
                switch (ui.path()) {
                    case "/about" -> buildAbout(ui);
                    default -> buildHome(ui);
                }"""));
    }

    private void buildWidgets(Ui ui) {
        ui.header(header -> header.title("Showcase / Widgets"));
        ui.title("Core widgets");
        ui.text("Each control keeps its value on the server and reruns this page when it changes.");

        boolean enabled = ui.checkbox("Enable notifications", true);
        double retries = ui.numberInput("Retries", 3.0, 0.0, 10.0, 1.0);
        String region = ui.selectbox("Region", java.util.List.of("US East", "EU West", "Asia Pacific"), 0);
        String plan = ui.radio("Plan", java.util.List.of("Free", "Team", "Enterprise"), 1);
        double volume = ui.slider("Volume", 0.0, 100.0, 50.0, 5.0);

        ui.markdown("Current settings: **" + enabled + "** · " + retries + " retries · " + region + " · " + plan
                + " · volume " + volume);
        ui.spinner("Refreshing widget preview", () -> ui.text("Preview refreshed."));
        if (ui.downloadButton("Download example", "Lumina widgets\n".getBytes(java.nio.charset.StandardCharsets.UTF_8), "widgets.txt")) {
            ui.text("Your download has started.");
        }
    }

    private void buildAi(Ui ui) {
        ui.header(header -> header.title("Showcase / AI extras"));
        ui.title("AI response metadata");
        ui.ai("Lumina keeps AI responses and their supporting evidence together.");
        ui.citation("Lumina guide", "/docs/GUIDE.md", "Use the provider SPI to stream an offline or Spring AI response.");
        ui.ragSources(java.util.List.of(java.util.Map.of(
                "title", "Architecture", "uri", "docs/ARCHITECTURE.md", "score", 0.94)));
        ui.toolCall("searchDocs", "complete", java.util.Map.of("query", "streaming"), java.util.Map.of("matches", 3));
        ui.usage(128, 64, 0.0012, 142L);
        ui.agentTimeline(java.util.List.of(
                java.util.Map.of("label", "Plan", "status", "complete"),
                java.util.Map.of("label", "Answer", "status", "streaming")));
        ui.memoryPanel(java.util.List.of(java.util.Map.of("key", "audience", "value", "Java teams")));
    }
}
