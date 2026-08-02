package io.lumina.examples.ai;

import io.lumina.ui.Ui;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MCP-style tool console UI. Tools are local demos; swap {@link DemoMcpCatalog} for a real MCP client.
 */
public final class McpConsolePages {
    private McpConsolePages() {}

    public static void build(Ui ui) {
        ui.title("MCP tool console");
        ui.markdown(
                "Browse **tools** as if discovered from an MCP server, invoke one, and inspect the "
                        + "result. Wire an MCP Java client underneath — Lumina stays the cockpit.");

        ui.markdown("### Discovered tools");
        ui.table(DemoMcpCatalog.asRows());

        List<String> names = DemoMcpCatalog.tools().stream().map(DemoMcpCatalog.Tool::name).toList();
        String selected = ui.selectbox("Tool", names, 0);
        String args = ui.textInput("Arguments");

        List<Map<String, Object>> history =
                ui.state().computeIfAbsent("mcp.history", k -> new ArrayList<>());

        if (ui.button("Call tool")) {
            long started = System.currentTimeMillis();
            DemoMcpCatalog.Tool tool = DemoMcpCatalog.require(selected);
            String output;
            String status = "complete";
            try {
                output = tool.handler().apply(args == null ? "" : args);
            } catch (RuntimeException ex) {
                status = "error";
                output = ex.getMessage() == null ? ex.toString() : ex.getMessage();
            }
            long latency = System.currentTimeMillis() - started;
            ui.toolCall(selected, status, Map.of("arguments", args == null ? "" : args), Map.of("result", output));
            ui.toolInvocation(selected, status, output);
            ui.usage(8, Math.max(1, output.length() / 4), 0.0, latency);
            history.add(0, Map.of(
                    "tool", selected,
                    "status", status,
                    "args", args == null ? "" : args,
                    "result", truncate(output, 80)));
            ui.state().set("mcp.history", history);
            ui.notify(status.equals("complete") ? "Tool completed" : "Tool failed");
            ui.json(Map.of("tool", selected, "status", status, "output", output));
        }

        if (!history.isEmpty()) {
            ui.markdown("### Recent calls");
            ui.table(history);
        }

        ui.expander("Connect a real MCP client", body -> body.code(
                "java",
                """
                // Pseudocode — use your MCP Java SDK
                List<McpTool> tools = mcpClient.listTools();
                ui.table(toRows(tools));
                McpResult result = mcpClient.call(selected, args);
                ui.toolCall(selected, "complete", args, result.payload());
                """));
    }

    private static String truncate(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max - 1) + "…";
    }
}
