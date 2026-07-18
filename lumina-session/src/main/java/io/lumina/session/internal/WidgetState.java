package io.lumina.session.internal;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores transient widget values and consumable user actions for one session.
 */
public final class WidgetState {
    private final Map<String, Object> values = new HashMap<>();

    /**
     * Creates empty widget state.
     */
    public WidgetState() {}

    /**
     * Returns a widget value.
     *
     * @param key widget key
     * @param <T> expected value type
     * @return stored value, or {@code null} if absent
     */
    @SuppressWarnings("unchecked")
    public <T> T value(String key) {
        return (T) values.get(key);
    }

    /**
     * Stores a widget value.
     *
     * @param key widget key
     * @param value value to store
     */
    public void set(String key, Object value) {
        values.put(key, value);
    }

    /**
     * Removes and reports a pending click.
     *
     * @param key widget key
     * @return {@code true} if a click was pending
     */
    public boolean consumeClick(String key) {
        return Boolean.TRUE.equals(values.remove(key));
    }

    /**
     * Removes and returns a pending chat submission.
     *
     * @param key widget key
     * @return submitted text, or {@code null} if absent
     */
    public String consumeChatSubmit(String key) {
        Object value = values.remove(key);
        return value instanceof String submission ? submission : null;
    }

    /**
     * Stores a pending chat submission.
     *
     * @param key widget key
     * @param submission submitted text
     */
    public void setChatSubmit(String key, String submission) {
        set(key, submission);
    }
}
