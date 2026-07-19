package io.lumina.ui;

import io.lumina.ai.TokenStream;
import io.lumina.state.StateStore;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Declarative UI binder for a single {@link io.lumina.LuminaApp#build} run.
 * Implementations are provided by the runtime; app code receives a fresh instance per rerun.
 */
public interface Ui {
    /**
     * Renders a page or section heading.
     *
     * @param text heading text; never null
     */
    void title(String text);

    /**
     * Renders Markdown-formatted rich text. Phase 1 supports headings ({@code #} through
     * {@code ######}) and line breaks only; raw HTML is always displayed as text.
     *
     * @param md Markdown source; never null
     */
    void markdown(String md);

    /**
     * Renders plain text content.
     *
     * @param text text to display; never null
     */
    void text(String text);

    /**
     * Renders a clickable button and reports whether it was activated this run.
     *
     * @param label button label; never null
     * @return {@code true} if the user clicked this button during the current run
     */
    boolean button(String label);

    /**
     * Renders a labeled single-line text field and returns its current value.
     *
     * @param label field label; never null
     * @return current field value; never null
     */
    String textInput(String label);

    /**
     * Renders a chat-style composer and returns a newly submitted prompt, if any.
     *
     * @return submitted prompt for this run, or {@code null} if the user did not submit
     */
    String chatInput();

    /**
     * Appends an end-user chat message bubble.
     *
     * @param message message text; never null
     */
    void user(String message);

    /**
     * Appends an AI assistant chat message bubble.
     *
     * @param message message text; never null
     */
    void ai(String message);

    /**
     * Renders an assistant chat message whose text streams in chunk-by-chunk. Blocks until the
     * stream completes and returns the fully accumulated text (for history persistence). The
     * accumulated text also becomes the message node's content.
     *
     * @param tokens streamed reply chunks; must not be null
     * @return the fully accumulated reply text
     */
    String ai(TokenStream tokens);

    /**
     * Renders a syntax-highlighted code block.
     *
     * @param language language identifier for highlighting; never null
     * @param source source code; never null
     */
    void code(String language, String source);

    /**
     * Renders a structured JSON viewer for the given value.
     *
     * @param value value to display; may be null
     */
    void json(Object value);

    /**
     * Renders tabular data from a list of row maps.
     *
     * @param rows table rows; each map keys column names to cell values; never null
     */
    void table(List<Map<String, Object>> rows);

    /**
     * Renders an image from a URL or classpath resource path.
     *
     * @param urlOrResource image location; never null
     */
    void image(String urlOrResource);

    /**
     * Renders a file upload control and returns an uploaded file when one arrived this run.
     *
     * @param label upload control label; never null
     * @return uploaded file for this run, or empty if none was received
     */
    Optional<UploadedFile> fileUpload(String label);

    /**
     * Renders a progress indicator.
     *
     * @param value completion fraction from {@code 0.0} through {@code 1.0} inclusive
     */
    void progress(double value);

    /**
     * Returns the session-scoped store for app-owned state that survives reruns.
     *
     * @return session state store; never null
     */
    StateStore state();

    /**
     * Runs a nested binder block under an explicit widget key for stable identity across reruns.
     *
     * @param key explicit widget key; never null
     * @param block nested UI declarations; never null
     * @param <T> result type produced by the block
     * @return value returned by the block
     */
    <T> T withKey(String key, Function<Ui, T> block);
}
