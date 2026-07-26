package io.lumina.examples.showcase;

import io.lumina.LuminaApp;
import io.lumina.ui.PageConfig;
import io.lumina.ui.PageLayout;
import io.lumina.ui.SidebarState;
import io.lumina.ui.Ui;

/**
 * Interactive hero demo: Streamlit-style rerun loop, session state, layout, and widgets — in pure Java.
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
            sidebar.markdown("## Lumina");
            sidebar.markdown(
                    "Build **interactive web apps in pure Java** — the Streamlit model for the JVM: "
                            + "declare UI, rerun on every interaction, keep state on the server.");
            sidebar.markdown("### How it works");
            sidebar.markdown(
                    "1. You write `build(Ui ui)` in Java\n"
                            + "2. Lumina renders a component tree\n"
                            + "3. Clicks and inputs trigger a **rerun**\n"
                            + "4. No HTML, CSS, or JavaScript from you");
            if (sidebar.button("Reset demo")) {
                state.remove("count");
                state.remove("progress");
                state.remove("greeting");
            }
        });

        ui.title("Streamlit-style apps in Java");
        ui.markdown(
                "Lumina is an open-source framework for **server-driven, interactive UIs**. "
                        + "Like Streamlit, you compose widgets in the host language and the runtime "
                        + "handles rendering, WebSocket updates, and session state.");

        ui.markdown("### Try the rerun loop");
        ui.text("Click a button — the server reruns `build()` and patches the UI in real time.");

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

        ui.markdown("### Inputs & layout");
        String name = ui.textInput("Your name");
        if (ui.button("Say hello")) {
            state.set("greeting", name.isBlank() ? "stranger" : name.trim());
        }
        String greeting = state.get("greeting");
        if (greeting != null) {
            ui.markdown("Hello, **" + greeting + "**! Session state remembered that across reruns.");
        }

        ui.expander("View the Java source", body -> body.code(
                "java",
                """
                ui.columns(2, cols -> {
                    cols[0].container(box -> {
                        if (box.button("Increment")) {
                            state.set("count", count + 1);
                        }
                        box.markdown("Count: **" + count + "**");
                    });
                    cols[1].progress(progress);
                });"""));

        ui.markdown("### Next examples");
        ui.markdown(
                "- **Hello AI** — stateful chat with `ui.chatInput()`\n"
                        + "- **Streaming chat** — token streaming with `ui.ai(TokenStream)`\n"
                        + "- Run: `mvn -pl lumina-examples exec:java -Dexec.mainClass=...`");
    }
}
