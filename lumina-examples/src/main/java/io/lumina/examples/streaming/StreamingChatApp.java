package io.lumina.examples.streaming;

import io.lumina.LuminaApp;
import io.lumina.ai.ChatClient;
import io.lumina.ai.ChatClients;
import io.lumina.ui.Ui;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal stateful chat application that streams replies from Lumina's offline echo client.
 */
public final class StreamingChatApp implements LuminaApp {
    private final ChatClient chat = ChatClients.echo();

    /**
     * Creates a Streaming Chat application using the built-in echo client.
     */
    public StreamingChatApp() {}

    @Override
    public void build(Ui ui) {
        ui.title("Streaming Chat");
        List<String[]> history = ui.state().computeIfAbsent("history", k -> new ArrayList<>());
        for (String[] turn : history) {
            ui.user(turn[0]);
            ui.ai(turn[1]);
        }
        String prompt = ui.chatInput();
        if (prompt != null) {
            ui.user(prompt);
            String reply = ui.ai(chat.stream(prompt));
            history.add(new String[] {prompt, reply});
        }
    }
}
