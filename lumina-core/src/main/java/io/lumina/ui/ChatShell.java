package io.lumina.ui;

import java.util.function.Consumer;

/**
 * Structured chat page regions: optional header, sticky composer, and scrollable transcript.
 */
public interface ChatShell {
    /** Optional chat chrome (title, theme toggle). */
    void header(Consumer<Ui> body);

    /** Composer region (typically {@link Ui#chatInput()}). */
    void composer(Consumer<Ui> body);

    /** Message transcript region ({@link Ui#user}, {@link Ui#ai}). */
    void transcript(Consumer<Ui> body);
}
