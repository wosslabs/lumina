package io.lumina.ui;

/**
 * Options for {@link Ui#chatShell(ChatShellOptions, java.util.function.Consumer)}.
 *
 * @param newestFirst when {@code true} (default), authors should render transcript turns newest→oldest
 *        and the client sticks the composer to the top
 */
public record ChatShellOptions(boolean newestFirst) {
    /** Defaults with newest-first transcript ordering. */
    public static ChatShellOptions defaults() {
        return new ChatShellOptions(true);
    }
}
