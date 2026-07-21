package io.lumina.runtime;

import static io.lumina.components.ComponentSpecs.CONTENT;
import static io.lumina.components.ComponentSpecs.LABEL;
import static io.lumina.components.ComponentSpecs.LANGUAGE;
import static io.lumina.components.ComponentSpecs.ROWS;
import static io.lumina.components.ComponentSpecs.SOURCE;
import static io.lumina.components.ComponentSpecs.SRC;
import static io.lumina.components.ComponentSpecs.VALUE;

import io.lumina.ai.TokenStream;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Builds an immutable component tree from one run of the {@link Ui} DSL.
 */
public final class UiBinder implements Ui {
    private static final String ROOT_PATH = "auto:";

    private record Frame(List<ComponentNode> children) {
        Frame() {
            this(new ArrayList<>());
        }
    }

    private final SessionState session;
    private final StreamBridge stream;
    private final Deque<Frame> frames = new ArrayDeque<>();
    private final Deque<String> paths = new ArrayDeque<>();
    private final Deque<Map<String, Integer>> counters = new ArrayDeque<>();
    private final Set<String> streamedIds = new LinkedHashSet<>();

    /**
     * Creates a binder backed by the supplied session state.
     *
     * @param session state owned by the current session
     */
    public UiBinder(SessionState session) {
        this(session, StreamBridge.NOOP);
    }

    /**
     * Creates a binder backed by the supplied session state, notifying {@code stream} while
     * streamed {@code ai_message} content is produced.
     *
     * @param session state owned by the current session
     * @param stream hook invoked while streaming an {@code ai_message}
     */
    public UiBinder(SessionState session, StreamBridge stream) {
        this.session = Objects.requireNonNull(session, "session");
        this.stream = Objects.requireNonNull(stream, "stream");
        paths.push(ROOT_PATH);
        counters.push(new HashMap<>());
        frames.push(new Frame());
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
    public String ai(TokenStream tokens) {
        Objects.requireNonNull(tokens, "tokens");
        String key = nextKey(ComponentTypes.AI_MESSAGE);
        List<ComponentNode> current = frames.peek().children();
        current.add(new ComponentNode(key, ComponentTypes.AI_MESSAGE, Map.of(CONTENT, ""), List.of()));
        stream.flushBefore(List.copyOf(current));
        stream.streamStart(key);
        StringBuilder acc = new StringBuilder();
        try {
            for (String chunk : tokens) {
                acc.append(chunk);
                stream.streamAppend(key, chunk);
            }
        } finally {
            stream.streamEnd(key);
        }
        streamedIds.add(key);
        int last = current.size() - 1;
        current.set(last, new ComponentNode(key, ComponentTypes.AI_MESSAGE, Map.of(CONTENT, acc.toString()), List.of()));
        return acc.toString();
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

    @Override
    public void container(Consumer<Ui> body) {
        throw new UnsupportedOperationException("Task 3");
    }

    @Override
    public void columns(int n, Consumer<Ui[]> cols) {
        throw new UnsupportedOperationException("Task 3");
    }

    @Override
    public void sidebar(Consumer<Ui> body) {
        throw new UnsupportedOperationException("Task 3");
    }

    @Override
    public boolean expander(String label, Consumer<Ui> body) {
        throw new UnsupportedOperationException("Task 3");
    }

    /**
     * Creates the immutable root containing all components declared during this run.
     *
     * @return root of the completed component tree
     */
    public ComponentNode buildRoot() {
        return new ComponentNode("root", ComponentTypes.ROOT, Map.of(), List.copyOf(frames.peek().children()));
    }

    /**
     * Returns the ids of {@code ai_message} nodes produced via {@link #ai(TokenStream)} this run.
     *
     * @return immutable snapshot of streamed node ids
     */
    Set<String> streamedNodeIds() {
        return Set.copyOf(streamedIds);
    }

    private String addNode(String type, Map<String, Object> props) {
        String key = nextKey(type);
        addNode(key, type, props);
        return key;
    }

    private void addNode(String key, String type, Map<String, Object> props) {
        frames.peek().children().add(new ComponentNode(key, type, props, List.of()));
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
