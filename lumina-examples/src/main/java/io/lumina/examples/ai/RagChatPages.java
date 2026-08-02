package io.lumina.examples.ai;

import io.lumina.ai.TokenStream;
import io.lumina.ui.Ui;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Interactive RAG chat UI. Retrieval is demo-local; swap {@link DemoCorpus} for Spring AI RAG.
 */
public final class RagChatPages {
    private RagChatPages() {}

    public static void build(Ui ui) {
        ui.title("RAG chat");
        ui.markdown(
                "Ask about Lumina. The app **retrieves** local docs, shows **sources**, then "
                        + "streams an answer. Replace retrieval with Spring AI / your vector store.");

        List<String[]> history = ui.state().computeIfAbsent("rag.history", k -> new ArrayList<>());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> lastSources =
                ui.state().computeIfAbsent("rag.lastSources", k -> new ArrayList<>());

        for (String[] turn : history) {
            ui.user(turn[0]);
            ui.ai(turn[1]);
        }
        if (!lastSources.isEmpty() && !history.isEmpty()) {
            ui.ragSources(lastSources);
            Map<String, Object> top = lastSources.get(0);
            ui.citation(
                    String.valueOf(top.get("title")),
                    String.valueOf(top.get("uri")),
                    String.valueOf(top.get("snippet")));
        }

        String prompt = ui.chatInput();
        if (prompt != null && !prompt.isBlank()) {
            long started = System.currentTimeMillis();
            List<Map<String, Object>> sources = DemoCorpus.retrieve(prompt, 3);
            ui.state().set("rag.lastSources", new ArrayList<>(sources));
            ui.user(prompt);
            ui.ragSources(sources);
            if (!sources.isEmpty()) {
                Map<String, Object> top = sources.get(0);
                ui.citation(
                        String.valueOf(top.get("title")),
                        String.valueOf(top.get("uri")),
                        String.valueOf(top.get("snippet")));
            }
            String grounded = DemoCorpus.answerFrom(prompt, sources);
            String reply = ui.ai(chunked(grounded));
            history.add(new String[] {prompt, reply});
            ui.usage(prompt.length(), reply.length(), 0.0, System.currentTimeMillis() - started);
            ui.toolCall(
                    "retriever.search",
                    "complete",
                    Map.of("query", prompt, "k", 3),
                    Map.of("hits", sources.size()));
        }

        ui.expander("How to wire Spring AI RAG", body -> body.code(
                "java",
                """
                // 1) Retrieve with Spring AI / your vector store
                List<Document> docs = retriever.retrieve(prompt);
                ui.ragSources(toSourceMaps(docs));
                // 2) Augment prompt and stream
                String reply = ui.ai(springAi.stream(augmentedPrompt(prompt, docs)));
                """));
    }

    static TokenStream chunked(String text) {
        List<String> parts = new ArrayList<>();
        int size = 24;
        for (int i = 0; i < text.length(); i += size) {
            parts.add(text.substring(i, Math.min(text.length(), i + size)));
        }
        return TokenStream.fromIterable(parts);
    }
}
