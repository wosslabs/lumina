package io.lumina.examples.agent;

import io.lumina.LuminaApp;
import io.lumina.ui.Ui;
import java.util.List;
import java.util.Map;

/**
 * Minimal human-in-the-loop agent interface.
 */
public final class AgentDemoApp implements LuminaApp {
    @Override
    public void build(Ui ui) {
        ui.title("Agent demo");
        ui.agentTimeline(List.of(
                Map.of("id", "plan", "label", "Plan response", "status", "complete"),
                Map.of("id", "tool", "label", "Search knowledge", "status", "complete")));
        ui.toolInvocation("searchKnowledge", "complete", "Found 3 relevant notes");
        if (ui.approval("Allow the agent to send this answer?")) {
            ui.notify("Answer approved.");
        }
        ui.memoryPanel(List.of(Map.of("topic", "Lumina", "preference", "concise")));
    }
}
