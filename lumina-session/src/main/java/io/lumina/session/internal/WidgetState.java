package io.lumina.session.internal;

import java.util.HashMap;
import java.util.Map;

public final class WidgetState {
    private final Map<String, Object> values = new HashMap<>();

    @SuppressWarnings("unchecked")
    public <T> T value(String key) {
        return (T) values.get(key);
    }

    public void set(String key, Object value) {
        values.put(key, value);
    }

    public boolean consumeClick(String key) {
        return Boolean.TRUE.equals(values.remove(key));
    }

    public String consumeChatSubmit(String key) {
        Object value = values.remove(key);
        return value instanceof String submission ? submission : null;
    }

    public void setChatSubmit(String key, String submission) {
        set(key, submission);
    }
}
