package io.lumina.session.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import io.lumina.state.StateStore;

public final class MapStateStore implements StateStore {
    private final Map<String, Object> values = new HashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) values.get(key);
    }

    @Override
    public void set(String key, Object value) {
        values.put(key, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T computeIfAbsent(String key, Function<String, T> mappingFunction) {
        return (T) values.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public boolean contains(String key) {
        return values.containsKey(key);
    }

    @Override
    public void remove(String key) {
        values.remove(key);
    }
}
