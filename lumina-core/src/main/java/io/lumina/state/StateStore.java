package io.lumina.state;

import java.util.function.Function;

/**
 * Session-scoped key/value store for app-owned state.
 */
public interface StateStore {
    /**
     * Returns the value associated with the given key.
     *
     * @param key state key; never null
     * @param <T> expected value type
     * @return stored value, or {@code null} if absent
     */
    <T> T get(String key);

    /**
     * Stores a value under the given key.
     *
     * @param key state key; never null
     * @param value value to store; may be null
     */
    void set(String key, Object value);

    /**
     * Returns the existing value or computes and stores one when absent.
     *
     * @param key state key; never null
     * @param mappingFunction factory invoked with the key when no value exists; never null
     * @param <T> value type
     * @return existing or newly computed value
     */
    <T> T computeIfAbsent(String key, Function<String, T> mappingFunction);

    /**
     * Reports whether a value is stored under the given key.
     *
     * @param key state key; never null
     * @return {@code true} if the key is present
     */
    boolean contains(String key);

    /**
     * Removes the value associated with the given key, if any.
     *
     * @param key state key; never null
     */
    void remove(String key);
}
