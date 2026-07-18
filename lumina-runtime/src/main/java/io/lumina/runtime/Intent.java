package io.lumina.runtime;

import java.util.Map;

/**
 * One client-originated action submitted to a {@link SessionHandle} for the next rerun.
 *
 * @param name intent kind: {@code connect}, {@code click}, {@code input}, {@code submit_chat}, or
 *     {@code file_upload}
 * @param targetId widget key this intent applies to, or {@code null} for {@code connect}
 * @param payload intent-specific data, e.g. {@code value} for text intents or {@code file} for an
 *     upload
 */
public record Intent(String name, String targetId, Map<String, Object> payload) {
    /**
     * Creates an intent and snapshots its payload.
     */
    public Intent {
        payload = Map.copyOf(payload);
    }

    /**
     * Creates the initial intent sent when a session is created.
     *
     * @return connect intent with no target or payload
     */
    public static Intent connect() {
        return new Intent("connect", null, Map.of());
    }

    /**
     * Creates a chat submission intent.
     *
     * @param targetId chat input widget key
     * @param text submitted prompt text
     * @return submit_chat intent
     */
    public static Intent chatSubmit(String targetId, String text) {
        return new Intent("submit_chat", targetId, Map.of("value", text));
    }

    /**
     * Creates a button click intent.
     *
     * @param targetId button widget key
     * @return click intent
     */
    public static Intent click(String targetId) {
        return new Intent("click", targetId, Map.of());
    }

    /**
     * Creates a text input change intent.
     *
     * @param targetId text input widget key
     * @param value new field value
     * @return input intent
     */
    public static Intent input(String targetId, String value) {
        return new Intent("input", targetId, Map.of("value", value));
    }
}
