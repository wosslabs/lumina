package io.lumina.examples.ai;

import io.lumina.ui.Ui;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Human-in-the-loop agent workbench: plan → tools → approval → answer → memory.
 */
public final class AgentWorkbenchPages {
    private AgentWorkbenchPages() {}

    public static void build(Ui ui) {
        ui.title("Agent workbench");
        ui.markdown(
                "A miniature agent loop with **timeline**, **tool rows**, **human approval**, and "
                        + "**memory**. Plug in Spring AI agents / LangChain4j for real planning.");

        var state = ui.state();
        List<Map<String, Object>> memory =
                state.computeIfAbsent(
                        "agent.memory",
                        k -> new ArrayList<>(List.of(
                                Map.of("key", "product", "value", "Lumina"),
                                Map.of("key", "tone", "value", "concise"))));
        String goal = state.computeIfAbsent("agent.goal", k -> "");
        boolean approved = Boolean.TRUE.equals(state.get("agent.approved"));
        boolean ranTools = Boolean.TRUE.equals(state.get("agent.ranTools"));
        String draft = state.computeIfAbsent("agent.draft", k -> "");

        String goalInput = ui.textInput("Agent goal");
        if (ui.button("Run agent plan") && !goalInput.isBlank()) {
            state.set("agent.goal", goalInput.trim());
            state.set("agent.ranTools", false);
            state.set("agent.approved", false);
            state.set("agent.draft", "");
            goal = goalInput.trim();
            ranTools = false;
            approved = false;
            draft = "";
        }

        if (goal == null || goal.isBlank()) {
            ui.text("Enter a goal (e.g. \"Summarize Lumina routing\") and click Run agent plan.");
            return;
        }

        ui.markdown("### Goal\n**" + goal + "**");

        if (!ranTools) {
            final String goalForTools = goal;
            ui.spinner("Running tools", () -> {
                var retrieved = DemoCorpus.retrieve(goalForTools, 2);
                state.set("agent.toolHits", retrieved);
                StringBuilder draftBuilder =
                        new StringBuilder("Draft answer for: ").append(goalForTools).append("\n\n");
                for (Map<String, Object> hit : retrieved) {
                    draftBuilder
                            .append("- ")
                            .append(hit.get("title"))
                            .append(": ")
                            .append(hit.get("snippet"))
                            .append('\n');
                }
                state.set("agent.draft", draftBuilder.toString());
                state.set("agent.ranTools", true);
            });
            ranTools = true;
            draft = state.get("agent.draft");
        }

        ui.agentTimeline(List.of(
                Map.of("id", "plan", "label", "Plan steps", "status", "complete"),
                Map.of("id", "tools", "label", "Call tools", "status", "complete"),
                Map.of(
                        "id",
                        "approve",
                        "label",
                        "Human approval",
                        "status",
                        approved ? "complete" : "pending"),
                Map.of(
                        "id",
                        "answer",
                        "label",
                        "Final answer",
                        "status",
                        approved ? "complete" : "blocked")));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> hits = state.computeIfAbsent("agent.toolHits", k -> List.of());
        ui.toolInvocation("docs.search", "complete", "Retrieved " + hits.size() + " chunks");
        ui.toolCall("docs.search", "complete", Map.of("goal", goal), Map.of("hits", hits.size()));
        if (!hits.isEmpty()) {
            ui.ragSources(hits);
        }

        ui.markdown("### Draft (pre-approval)");
        ui.code("markdown", draft == null ? "" : draft);

        if (!approved) {
            if (ui.approval("Allow the agent to publish this answer to the user?")) {
                state.set("agent.approved", true);
                memory.add(Map.of("key", "last_goal", "value", goal));
                state.set("agent.memory", memory);
                ui.notify("Answer approved and published.");
                approved = true;
            } else {
                ui.text("Waiting for approval…");
            }
        }

        if (approved) {
            ui.markdown("### Published answer");
            ui.ai(draft == null ? "" : draft);
            ui.usage(120, Math.max(1, draft == null ? 1 : draft.length() / 4), 0.0, 90L);
        }

        ui.memoryPanel(memory);

        if (ui.button("Reset agent session")) {
            state.remove("agent.goal");
            state.remove("agent.ranTools");
            state.remove("agent.approved");
            state.remove("agent.draft");
            state.remove("agent.toolHits");
        }
    }
}
