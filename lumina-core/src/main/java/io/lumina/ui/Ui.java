package io.lumina.ui;

import io.lumina.ai.TokenStream;
import io.lumina.state.StateStore;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Declarative UI binder for a single {@link io.lumina.LuminaApp#build} run.
 * Implementations are provided by the runtime; app code receives a fresh instance per rerun.
 */
public interface Ui {
    /**
     * Configures page title and layout shell. Must be invoked before any other {@code Ui} method in
     * this {@code build()} run.
     *
     * @param config page configuration; never null
     */
    void pageConfig(PageConfig config);

    /**
     * Returns the current server-side route path for this session (default {@code "/"}).
     *
     * @return normalized path, never null
     */
    String path();

    /**
     * Navigates to {@code path} for subsequent rendering in this run and future reruns.
     *
     * @param path absolute path starting with {@code /}; never null
     */
    void navigate(String path);

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
     * Renders a checkbox with an unchecked default.
     *
     * @param label visible control label; never null
     * @return current checked state
     */
    boolean checkbox(String label);

    /**
     * Renders a checkbox with the supplied initial state.
     *
     * @param label visible control label; never null
     * @param value initial checked state
     * @return current checked state
     */
    boolean checkbox(String label, boolean value);

    double numberInput(String label);

    double numberInput(String label, double value);

    double numberInput(String label, double value, double min, double max, double step);

    String selectbox(String label, List<String> options);

    String selectbox(String label, List<String> options, int index);

    String radio(String label, List<String> options);

    String radio(String label, List<String> options, int index);

    double slider(String label, double min, double max);

    double slider(String label, double min, double max, double value);

    double slider(String label, double min, double max, double value, double step);

    /**
     * Shows a transient activity indicator while the body executes.
     *
     * @param label activity label; never null
     * @param body operation to execute; never null
     */
    void spinner(String label, Runnable body);

    /**
     * Renders a download action.
     *
     * @param label button label; never null
     * @param data file contents; no more than 1 MiB
     * @param fileName download filename; never null
     * @return whether the action was clicked in this run
     */
    boolean downloadButton(String label, byte[] data, String fileName);

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

    /** Groups widgets in a generic block container. */
    void container(Consumer<Ui> body);

    /**
     * Lays out {@code n} equal-width columns. {@code cols[i]} is the {@code Ui} scoped to column
     * {@code i}. {@code n} must be {@code >= 1}.
     *
     * @param n number of columns; must be {@code >= 1}
     * @param cols callback receiving one {@code Ui} per column; never null
     */
    void columns(int n, Consumer<Ui[]> cols);

    /**
     * Renders a left sidebar rail (at most once per {@code build()}). Prefer structured
     * {@link SidebarUi#brand}, {@link SidebarUi#nav}, and {@link SidebarUi#footer} slots; freeform
     * widgets on {@code body} remain supported for legacy apps.
     *
     * @param body sidebar content declarations; never null
     */
    void sidebar(Consumer<SidebarUi> body);

    /**
     * Optional app header context line for the current view (not the page H1).
     *
     * @param body header declarations; never null
     */
    void header(Consumer<HeaderUi> body);

    /**
     * Collapsible section with {@code label}. Returns whether the expander is open after this run
     * (including any toggle intent applied before the rebuild). Open/closed persists in session
     * widget state keyed by the expander's node id.
     *
     * @param label expander heading; never null
     * @param body content shown when open; never null
     * @return {@code true} if the expander is open after this run
     */
    boolean expander(String label, Consumer<Ui> body);
}
