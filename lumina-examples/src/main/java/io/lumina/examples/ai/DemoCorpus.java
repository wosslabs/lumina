package io.lumina.examples.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Tiny in-memory corpus used by the RAG demos. Real apps replace this with Spring AI
 * retrievers / vector stores; Lumina only renders citations and sources.
 */
public final class DemoCorpus {
    public record Doc(String id, String title, String uri, String text) {}

    private static final List<Doc> DOCS = List.of(
            new Doc(
                    "arch",
                    "Architecture",
                    "docs/ARCHITECTURE.md",
                    "Lumina is a server-driven UI framework. The server owns the component tree, "
                            + "diffs patches over WebSocket, and keeps session state. Application "
                            + "authors declare widgets in Java with zero HTML CSS or JavaScript."),
            new Doc(
                    "ai",
                    "AI surfaces",
                    "docs/AI_GUIDE.md",
                    "Lumina provides chat streaming citations RAG source panels tool calls usage "
                            + "metrics agent timelines approvals and memory panels. Orchestration "
                            + "stays in Spring AI MCP clients or your own agent loop."),
            new Doc(
                    "spring",
                    "Spring Boot integration",
                    "docs/DEVELOPER_GUIDE.md",
                    "Register a LuminaApp bean with lumina-spring-boot-starter. Use lumina-spring-ai "
                            + "SpringAiChatClient to stream Spring AI ChatClient replies into ui.ai "
                            + "TokenStream. Keep API keys in environment variables."),
            new Doc(
                    "shell",
                    "Enterprise shell",
                    "docs/UX_CONSTITUTION.md",
                    "Use sidebar brand nav footer and ui.header for an accessible enterprise shell. "
                            + "Routing uses ui.path and ui.navigate with client pathname sync."));

    private DemoCorpus() {}

    public static List<Doc> all() {
        return DOCS;
    }

    /**
     * Naive keyword ranker — demo only. Production RAG should use embeddings / Spring AI.
     */
    public static List<Map<String, Object>> retrieve(String query, int limit) {
        String q = query == null ? "" : query.toLowerCase(Locale.ROOT);
        List<Scored> scored = new ArrayList<>();
        for (Doc doc : DOCS) {
            int score = 0;
            for (String token : q.split("\\W+")) {
                if (token.isBlank()) {
                    continue;
                }
                if (doc.title().toLowerCase(Locale.ROOT).contains(token)) {
                    score += 3;
                }
                if (doc.text().toLowerCase(Locale.ROOT).contains(token)) {
                    score += 1;
                }
            }
            if (score > 0 || q.isBlank()) {
                scored.add(new Scored(doc, q.isBlank() ? 0.5 : Math.min(0.99, score / 10.0)));
            }
        }
        scored.sort(Comparator.comparingDouble(Scored::score).reversed());
        return scored.stream()
                .limit(limit)
                .map(s -> Map.<String, Object>of(
                        "title", s.doc().title(),
                        "uri", s.doc().uri(),
                        "score", s.score(),
                        "snippet", snippet(s.doc().text(), 140)))
                .toList();
    }

    public static String answerFrom(String query, List<Map<String, Object>> sources) {
        if (sources.isEmpty()) {
            return "I could not find matching docs for \"" + query + "\". Try words like architecture, "
                    + "streaming, Spring, or shell.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Based on the retrieved Lumina docs for \"")
                .append(query)
                .append("\":\n\n");
        for (Map<String, Object> source : sources) {
            sb.append("- **")
                    .append(source.get("title"))
                    .append("** (")
                    .append(source.get("uri"))
                    .append("): ")
                    .append(source.get("snippet"))
                    .append('\n');
        }
        sb.append("\n_Lumina rendered the sources below; your production app would call an LLM "
                + "with these chunks via Spring AI._");
        return sb.toString();
    }

    private static String snippet(String text, int max) {
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max - 1) + "…";
    }

    private record Scored(Doc doc, double score) {}
}
