package io.lumina.runtime;

import io.lumina.LuminaException;
import io.lumina.state.StateStore;

/** Framework-owned session route path stored in {@link StateStore}. */
final class SessionRoutes {
    static final String PATH_KEY = "__lumina.path";

    private SessionRoutes() {}

    static String current(StateStore store) {
        Object value = store.get(PATH_KEY);
        return value instanceof String path ? path : "/";
    }

    static void set(StateStore store, String path) {
        store.set(PATH_KEY, normalize(path));
    }

    static String normalize(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    static String requirePayloadPath(Intent intent) {
        Object path = intent.payload().get("path");
        if (!(path instanceof String text) || text.isBlank()) {
            throw new LuminaException("Intent '" + intent.name() + "' requires payload path");
        }
        return text;
    }
}
