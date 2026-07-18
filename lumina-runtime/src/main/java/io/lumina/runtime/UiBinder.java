package io.lumina.runtime;

import static io.lumina.components.ComponentSpecs.CONTENT;
import static io.lumina.components.ComponentSpecs.LABEL;
import static io.lumina.components.ComponentSpecs.LANGUAGE;
import static io.lumina.components.ComponentSpecs.ROWS;
import static io.lumina.components.ComponentSpecs.SOURCE;
import static io.lumina.components.ComponentSpecs.SRC;
import static io.lumina.components.ComponentSpecs.VALUE;

import io.lumina.model.ComponentNode;
import io.lumina.model.ComponentTypes;
import io.lumina.session.internal.SessionState;
import io.lumina.state.StateStore;
import io.lumina.ui.Ui;
import io.lumina.ui.UploadedFile;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Builds an immutable component tree from one run of the {@link Ui} DSL.
 */
public final class UiBinder implements Ui {
    private static final String ROOT_PATH = "auto:";

    private final SessionState session;
    private final List<ComponentNode> children = new ArrayList<>();
    private final Deque<String> paths = new ArrayDeque<>();
    private final Deque<Map<String, Integer>> counters = new ArrayDeque<>();

    public UiBinder(SessionState session) {
        this.session = Objects.requireNonNull(session, "session");
        paths.push(ROOT_PATH);
        counters.push(new HashMap<>());
    }

    @Override
    public void title(String text) {
        addNode(ComponentTypes.TITLE, Map.of(CONTENT, text));
    }

    @Override
    public void markdown(String md) {
        addNode(ComponentTypes.MARKDOWN, Map.of(CONTENT, md));
    }

    @Override
    public void text(String text) {
        addNode(ComponentTypes.TEXT, Map.of(CONTENT, text));
    }

    @Override
    public boolean button(String label) {
        String key = addNode(ComponentTypes.BUTTON, Map.of(LABEL, label));
        return session.widgets().consumeClick(key);
    }

    @Override
    public String textInput(String label) {
        String key = nextKey(ComponentTypes.TEXT_INPUT);
        Object stored = session.widgets().value(key);
        String value = stored instanceof String text ? text : "";
        addNode(key, ComponentTypes.TEXT_INPUT, Map.of(LABEL, label, VALUE, value));
        return value;
    }

    @Override
    public String chatInput() {
        String key = addNode(ComponentTypes.CHAT_INPUT, Map.of());
        return session.widgets().consumeChatSubmit(key);
    }

    @Override
    public void user(String message) {
        addNode(ComponentTypes.USER_MESSAGE, Map.of(CONTENT, message));
    }

    @Override
    public void ai(String message) {
        addNode(ComponentTypes.AI_MESSAGE, Map.of(CONTENT, message));
    }

    @Override
    public void code(String language, String source) {
        addNode(ComponentTypes.CODE, Map.of(LANGUAGE, language, SOURCE, source));
    }

    @Override
    public void json(Object value) {
        addNode(ComponentTypes.JSON, Map.of(VALUE, snapshotJsonValue(value)));
    }

    @Override
    public void table(List<Map<String, Object>> rows) {
        addNode(ComponentTypes.TABLE, Map.of(ROWS, snapshotJsonValue(rows)));
    }

    @Override
    public void image(String urlOrResource) {
        addNode(ComponentTypes.IMAGE, Map.of(SRC, urlOrResource));
    }

    @Override
    public Optional<UploadedFile> fileUpload(String label) {
        String key = nextKey(ComponentTypes.FILE_UPLOAD);
        Object stored = session.widgets().value(key);
        session.widgets().set(key, null);
        addNode(key, ComponentTypes.FILE_UPLOAD, Map.of(LABEL, label));
        return stored instanceof UploadedFile file ? Optional.of(file) : Optional.empty();
    }

    @Override
    public void progress(double value) {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException("progress must be between 0.0 and 1.0 inclusive");
        }
        addNode(ComponentTypes.PROGRESS, Map.of(VALUE, value));
    }

    @Override
    public StateStore state() {
        return session.store();
    }

    @Override
    public <T> T withKey(String key, Function<Ui, T> block) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(block, "block");
        paths.push(paths.peek() + "/" + key);
        counters.push(new HashMap<>());
        try {
            return block.apply(this);
        } finally {
            counters.pop();
            paths.pop();
        }
    }

    public ComponentNode buildRoot() {
        return new ComponentNode("root", ComponentTypes.ROOT, Map.of(), children);
    }

    private String addNode(String type, Map<String, Object> props) {
        String key = nextKey(type);
        addNode(key, type, props);
        return key;
    }

    private void addNode(String key, String type, Map<String, Object> props) {
        children.add(new ComponentNode(key, type, props, List.of()));
    }

    private String nextKey(String type) {
        int index = counters.peek().merge(type, 1, Integer::sum) - 1;
        return paths.peek() + "/" + type + "#" + index;
    }

    private Object snapshotJsonValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<Object, Object> snapshot = new LinkedHashMap<>();
            map.forEach((key, entryValue) -> snapshot.put(key, snapshotJsonValue(entryValue)));
            return Collections.unmodifiableMap(snapshot);
        }
        if (value instanceof List<?> list) {
            List<Object> snapshot = new ArrayList<>(list.size());
            list.forEach(entry -> snapshot.add(snapshotJsonValue(entry)));
            return Collections.unmodifiableList(snapshot);
        }
        if (value instanceof String
                || value instanceof Number
                || value instanceof Boolean) {
            return value;
        }
        return String.valueOf(value);
    }
}
