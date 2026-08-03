package io.lumina.examples.springai;

import io.lumina.LuminaApp;
import io.lumina.ai.ChatClient;
import io.lumina.ai.ChatClients;
import io.lumina.springai.SpringAiChatClient;
import io.lumina.ui.PageConfig;
import io.lumina.ui.PageLayout;
import io.lumina.ui.Ui;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SpringAiExampleApp {
    public static void main(String[] args) {
        SpringApplication.run(SpringAiExampleApp.class, args);
    }

    @Bean
    @ConditionalOnMissingBean(ChatModel.class)
    ChatClient echoChatClient() {
        return ChatClients.echo();
    }

    @Bean
    LuminaApp luminaApp(ChatClient chat) {
        boolean offline = !(chat instanceof SpringAiChatClient);
        return ui -> buildChat(ui, chat, offline);
    }

    private static void buildChat(Ui ui, ChatClient chat, boolean offline) {
        ui.pageConfig(PageConfig.builder()
                .title("Path C — Spring Boot + AI")
                .layout(PageLayout.CHAT)
                .build());

        List<String[]> history = ui.state().computeIfAbsent("history", k -> new ArrayList<>());
        AtomicReference<String> promptRef = new AtomicReference<>();

        ui.chatShell(shell -> {
            shell.header(h -> {
                h.title("AI Chat");
                h.themeToggle();
                if (offline) {
                    h.markdown(
                            "Running with offline echo — set `OPENAI_API_KEY` for live model.");
                }
            });
            shell.composer(c -> promptRef.set(c.chatInput()));
            shell.transcript(t -> {
                String prompt = promptRef.get();
                if (prompt != null) {
                    t.user(prompt);
                    String reply = t.ai(chat.stream(prompt));
                    history.add(new String[] {prompt, reply});
                }
                int end = history.size() - (prompt != null ? 1 : 0);
                for (int i = end - 1; i >= 0; i--) {
                    t.user(history.get(i)[0]);
                    t.ai(history.get(i)[1]);
                }
            });
        });
    }
}
