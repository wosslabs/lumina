package io.lumina.examples.ai;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Simulated MCP-style tool catalog. Real apps discover tools from an MCP server; Lumina
 * renders the catalog, invocations, and results.
 */
public final class DemoMcpCatalog {
    public record Tool(String name, String description, Function<String, String> handler) {}

    private static final Map<String, Tool> TOOLS = new LinkedHashMap<>();

    static {
        register("docs.search", "Search Lumina documentation snippets", query -> {
            var hits = DemoCorpus.retrieve(query, 3);
            if (hits.isEmpty()) {
                return "No hits for: " + query;
            }
            StringBuilder sb = new StringBuilder();
            for (Map<String, Object> hit : hits) {
                sb.append(hit.get("title")).append(" → ").append(hit.get("uri")).append('\n');
            }
            return sb.toString().trim();
        });
        register("session.echo", "Echo a payload (connectivity check)", payload -> "pong: " + payload);
        register(
                "time.now",
                "Return the current UTC instant",
                ignored -> java.time.Instant.now().toString());
        register("math.add", "Add two integers separated by space, e.g. '3 5'", input -> {
            String[] parts = input.trim().split("\\s+");
            if (parts.length != 2) {
                return "usage: math.add <a> <b>";
            }
            try {
                long a = Long.parseLong(parts[0]);
                long b = Long.parseLong(parts[1]);
                return Long.toString(a + b);
            } catch (NumberFormatException ex) {
                return "both arguments must be integers";
            }
        });
    }

    private DemoMcpCatalog() {}

    private static void register(String name, String description, Function<String, String> handler) {
        TOOLS.put(name, new Tool(name, description, handler));
    }

    public static List<Tool> tools() {
        return List.copyOf(TOOLS.values());
    }

    public static Tool require(String name) {
        Tool tool = TOOLS.get(name);
        if (tool == null) {
            throw new IllegalArgumentException("Unknown tool: " + name);
        }
        return tool;
    }

    public static List<Map<String, Object>> asRows() {
        return tools().stream()
                .map(t -> Map.<String, Object>of("tool", t.name(), "description", t.description()))
                .toList();
    }
}
