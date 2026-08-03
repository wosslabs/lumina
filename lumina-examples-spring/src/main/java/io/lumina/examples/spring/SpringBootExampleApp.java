package io.lumina.examples.spring;

import io.lumina.LuminaApp;
import io.lumina.ui.PageConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SpringBootExampleApp {
    public static void main(String[] args) {
        SpringApplication.run(SpringBootExampleApp.class, args);
    }

    @Bean
    LuminaApp luminaApp() {
        return ui -> {
            ui.pageConfig(PageConfig.builder().title("Path B — Spring Boot").build());
            ui.title("Spring Boot + Lumina");
            String name = ui.textInput("Name");
            if (ui.button("Greet") && !name.isBlank()) {
                ui.state().set("greeting", name.trim());
            }
            Object greeting = ui.state().get("greeting");
            if (greeting instanceof String g && !g.isBlank()) {
                ui.markdown("Hello, **" + g + "**");
            }
            ui.markdown("Open Lumina at `http://127.0.0.1:8090/` (not Tomcat :8080).");
        };
    }
}
