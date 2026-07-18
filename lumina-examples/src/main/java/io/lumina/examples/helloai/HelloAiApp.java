package io.lumina.examples.helloai;

import io.lumina.LuminaApp;
import io.lumina.ai.ChatClient;
import io.lumina.ai.ChatClients;
import io.lumina.ui.Ui;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal stateful chat application backed by Lumina's offline echo client.
 */
public final class HelloAiApp implements LuminaApp {
    private final ChatClient chat = ChatClients.echo();

    /**
     * Creates a Hello AI application using the built-in echo client.
     */
    public HelloAiApp() {}

    @Override
    public void build(Ui ui) {
        ui.title("Hello AI");
        List<String[]> history = ui.state().computeIfAbsent("history", k -> new ArrayList<>());
        for (String[] turn : history) {
            ui.user(turn[0]);
            ui.ai(turn[1]);
        }
        String prompt = ui.chatInput();
        if (prompt != null) {
            String reply = chat.prompt(prompt);
            history.add(new String[] {prompt, reply});
            ui.user(prompt);
            ui.ai(reply);
        }
    }
}
