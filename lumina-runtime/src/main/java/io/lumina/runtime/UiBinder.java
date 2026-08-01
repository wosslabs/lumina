package io.lumina.runtime;

import static io.lumina.components.ComponentSpecs.CONTENT;
import static io.lumina.components.ComponentSpecs.COUNT;
import static io.lumina.components.ComponentSpecs.INDEX;
import static io.lumina.components.ComponentSpecs.LABEL;
import static io.lumina.components.ComponentSpecs.LANGUAGE;
import static io.lumina.components.ComponentSpecs.LAYOUT;
import static io.lumina.components.ComponentSpecs.OPEN;
import static io.lumina.components.ComponentSpecs.PAGE_TITLE;
import static io.lumina.components.ComponentSpecs.PATH;
import static io.lumina.components.ComponentSpecs.ROWS;
import static io.lumina.components.ComponentSpecs.SIDEBAR_STATE;
import static io.lumina.components.ComponentSpecs.SOURCE;
import static io.lumina.components.ComponentSpecs.SRC;
import static io.lumina.components.ComponentSpecs.VALUE;

import io.lumina.LuminaException;
import io.lumina.ai.TokenStream;
import io.lumina.model.ComponentNode;
import io.lumina.model.ComponentTypes;
import io.lumina.session.internal.SessionState;
import io.lumina.state.StateStore;
import io.lumina.ui.PageConfig;
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
import java.util.function.Supplier;

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

    private record PendingLayout(String id, String type, Map<String, Object> props) {}

    private record OpenColumns(String columnsId, int count, String[] colKeys, Frame[] colFrames) {}

    private final SessionState session;
    private final StreamBridge stream;
    private final Deque<Frame> frames = new ArrayDeque<>();
    private final Deque<String> paths = new ArrayDeque<>();
    private final Deque<Map<String, Integer>> counters = new ArrayDeque<>();
    private final Deque<PendingLayout> pendingLayouts = new ArrayDeque<>();
    private final Set<String> streamedIds = new LinkedHashSet<>();
    private OpenColumns openColumns;
    private boolean sidebarUsed;
    private PageConfig pageConfig;
    private boolean pageConfigLocked;

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
    public void pageConfig(PageConfig config) {
        Objects.requireNonNull(config, "config");
        if (pageConfigLocked) {
            throw new LuminaException("pageConfig() must be the first Ui call in build()");
        }
        this.pageConfig = config;
    }

    @Override
    public String path() {
        return SessionRoutes.current(session.store());
    }

    @Override
    public void navigate(String routePath) {
        Objects.requireNonNull(routePath, "path");
        SessionRoutes.set(session.store(), routePath);
    }

    @Override
    public void title(String text) {
        lockPageConfig();
        addNode(ComponentTypes.TITLE, Map.of(CONTENT, text));
    }

    @Override
    public void markdown(String md) {
        lockPageConfig();
        addNode(ComponentTypes.MARKDOWN, Map.of(CONTENT, md));
    }

    @Override
    public void text(String text) {
        lockPageConfig();
        addNode(ComponentTypes.TEXT, Map.of(CONTENT, text));
    }

    @Override
    public boolean button(String label) {
        lockPageConfig();
        String key = addNode(ComponentTypes.BUTTON, Map.of(LABEL, label));
        return session.widgets().consumeClick(key);
    }

    @Override
    public String textInput(String label) {
        lockPageConfig();
        String key = nextKey(ComponentTypes.TEXT_INPUT);
        Object stored = session.widgets().value(key);
        String value = stored instanceof String text ? text : "";
        addNode(key, ComponentTypes.TEXT_INPUT, Map.of(LABEL, label, VALUE, value));
        return value;
    }

    @Override
    public String chatInput() {
        lockPageConfig();
        String key = addNode(ComponentTypes.CHAT_INPUT, Map.of());
        return session.widgets().consumeChatSubmit(key);
    }

    @Override
    public void user(String message) {
        lockPageConfig();
        addNode(ComponentTypes.USER_MESSAGE, Map.of(CONTENT, message));
    }

    @Override
    public void ai(String message) {
        lockPageConfig();
        addNode(ComponentTypes.AI_MESSAGE, Map.of(CONTENT, message));
    }

    @Override
    public String ai(TokenStream tokens) {
        lockPageConfig();
        Objects.requireNonNull(tokens, "tokens");
        String key = nextKey(ComponentTypes.AI_MESSAGE);
        List<ComponentNode> current = targetChildren();
        current.add(new ComponentNode(key, ComponentTypes.AI_MESSAGE, Map.of(CONTENT, ""), List.of()));
        stream.flushBefore(snapshotInterimRoot());
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
        lockPageConfig();
        addNode(ComponentTypes.CODE, Map.of(LANGUAGE, language, SOURCE, source));
    }

    @Override
    public void json(Object value) {
        lockPageConfig();
        addNode(ComponentTypes.JSON, Map.of(VALUE, snapshotJsonValue(value)));
    }

    @Override
    public void table(List<Map<String, Object>> rows) {
        lockPageConfig();
        addNode(ComponentTypes.TABLE, Map.of(ROWS, snapshotJsonValue(rows)));
    }

    @Override
    public void image(String urlOrResource) {
        lockPageConfig();
        addNode(ComponentTypes.IMAGE, Map.of(SRC, urlOrResource));
    }

    @Override
    public Optional<UploadedFile> fileUpload(String label) {
        lockPageConfig();
        String key = nextKey(ComponentTypes.FILE_UPLOAD);
        Object stored = session.widgets().value(key);
        session.widgets().set(key, null);
        addNode(key, ComponentTypes.FILE_UPLOAD, Map.of(LABEL, label));
        return stored instanceof UploadedFile file ? Optional.of(file) : Optional.empty();
    }

    @Override
    public void progress(double value) {
        lockPageConfig();
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException("progress must be between 0.0 and 1.0 inclusive");
        }
        addNode(ComponentTypes.PROGRESS, Map.of(VALUE, value));
    }

    @Override
    public StateStore state() {
        lockPageConfig();
        return session.store();
    }

    @Override
    public <T> T withKey(String key, Function<Ui, T> block) {
        lockPageConfig();
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
        lockPageConfig();
        Objects.requireNonNull(body, "body");
        String id = nextKey(ComponentTypes.CONTAINER);
        List<ComponentNode> children = withLayoutFrame(id, ComponentTypes.CONTAINER, Map.of(), () -> body.accept(this));
        frames.peek().children().add(new ComponentNode(id, ComponentTypes.CONTAINER, Map.of(), children));
    }

    @Override
    public void columns(int n, Consumer<Ui[]> cols) {
        lockPageConfig();
        if (n < 1) {
            throw new IllegalArgumentException("columns requires n >= 1");
        }
        Objects.requireNonNull(cols, "cols");
        String columnsId = nextKey(ComponentTypes.COLUMNS);
        String[] colKeys = new String[n];
        Frame[] colFrames = new Frame[n];
        Ui[] scopes = new Ui[n];
        for (int i = 0; i < n; i++) {
            colKeys[i] = nextKey(ComponentTypes.COLUMN);
            colFrames[i] = new Frame();
            scopes[i] = new ColumnScopedUi(this, colFrames[i], colKeys[i]);
        }
        openColumns = new OpenColumns(columnsId, n, colKeys, colFrames);
        try {
            cols.accept(scopes);
        } finally {
            openColumns = null;
        }
        List<ComponentNode> columnNodes = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            columnNodes.add(new ComponentNode(
                    colKeys[i], ComponentTypes.COLUMN, Map.of(INDEX, i), colFrames[i].children()));
        }
        frames.peek()
                .children()
                .add(new ComponentNode(columnsId, ComponentTypes.COLUMNS, Map.of(COUNT, n), columnNodes));
    }

    @Override
    public void sidebar(Consumer<Ui> body) {
        lockPageConfig();
        Objects.requireNonNull(body, "body");
        if (sidebarUsed) {
            throw new LuminaException("Only one sidebar is allowed per build()");
        }
        sidebarUsed = true;
        String id = nextKey(ComponentTypes.SIDEBAR);
        List<ComponentNode> children = withLayoutFrame(id, ComponentTypes.SIDEBAR, Map.of(), () -> body.accept(this));
        frames.peek().children().add(new ComponentNode(id, ComponentTypes.SIDEBAR, Map.of(), children));
    }

    @Override
    public boolean expander(String label, Consumer<Ui> body) {
        lockPageConfig();
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(body, "body");
        String key = nextKey(ComponentTypes.EXPANDER);
        boolean open = Boolean.TRUE.equals(session.widgets().value(key));
        List<ComponentNode> children =
                withLayoutFrame(key, ComponentTypes.EXPANDER, Map.of(LABEL, label, OPEN, open), () -> body.accept(this));
        frames.peek()
                .children()
                .add(new ComponentNode(key, ComponentTypes.EXPANDER, Map.of(LABEL, label, OPEN, open), children));
        return open;
    }

    /**
     * Creates the immutable root containing all components declared during this run.
     *
     * @return root of the completed component tree
     */
    public ComponentNode buildRoot() {
        return new ComponentNode("root", ComponentTypes.ROOT, rootProps(),
                List.copyOf(frames.peek().children()));
    }

    private Map<String, Object> rootProps() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put(PATH, SessionRoutes.current(session.store()));
        if (pageConfig == null) {
            return Map.copyOf(props);
        }
        if (!pageConfig.title().isBlank()) {
            props.put(PAGE_TITLE, pageConfig.title());
        }
        props.put(LAYOUT, pageConfig.layout().wireValue());
        props.put(SIDEBAR_STATE, pageConfig.sidebarState().wireValue());
        return Map.copyOf(props);
    }

    private void lockPageConfig() {
        pageConfigLocked = true;
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
        targetChildren().add(new ComponentNode(key, type, props, List.of()));
    }

    private List<ComponentNode> targetChildren() {
        return frames.peek().children();
    }

    private String nextKey(String type) {
        int index = counters.peek().merge(type, 1, Integer::sum) - 1;
        return paths.peek() + "/" + type + "#" + index;
    }

    private List<ComponentNode> withLayoutFrame(
            String id, String type, Map<String, Object> props, Runnable block) {
        Frame frame = new Frame();
        frames.push(frame);
        pendingLayouts.push(new PendingLayout(id, type, props));
        try {
            block.run();
        } finally {
            pendingLayouts.pop();
            frames.pop();
        }
        return frame.children();
    }

    private ComponentNode snapshotInterimRoot() {
        if (openColumns != null && frames.size() >= 2) {
            return wrapPendingLayouts(levelWithOpenColumnsPartial());
        }
        List<Frame> framesBottomUp = bottomUp(frames);
        if (framesBottomUp.size() == 1 && pendingLayouts.isEmpty()) {
            return new ComponentNode(
                    "root", ComponentTypes.ROOT, rootProps(), List.copyOf(framesBottomUp.getFirst().children()));
        }
        List<ComponentNode> nodes = List.copyOf(framesBottomUp.getLast().children());
        return wrapPendingLayouts(nodes);
    }

    private List<ComponentNode> levelWithOpenColumnsPartial() {
        List<Frame> stack = bottomUp(frames);
        Frame parent = stack.get(stack.size() - 2);
        List<ComponentNode> level = new ArrayList<>(parent.children());
        level.add(buildOpenColumnsPartial());
        return level;
    }

    private ComponentNode buildOpenColumnsPartial() {
        Frame activeColFrame = frames.peek();
        List<ComponentNode> columnNodes = new ArrayList<>(openColumns.count());
        for (int i = 0; i < openColumns.count(); i++) {
            List<ComponentNode> colChildren = openColumns.colFrames()[i] == activeColFrame
                    ? List.copyOf(activeColFrame.children())
                    : List.copyOf(openColumns.colFrames()[i].children());
            columnNodes.add(new ComponentNode(
                    openColumns.colKeys()[i], ComponentTypes.COLUMN, Map.of(INDEX, i), colChildren));
        }
        return new ComponentNode(
                openColumns.columnsId(), ComponentTypes.COLUMNS, Map.of(COUNT, openColumns.count()), columnNodes);
    }

    private ComponentNode wrapPendingLayouts(List<ComponentNode> innermostLevel) {
        List<Frame> framesBottomUp = bottomUp(frames);
        List<PendingLayout> layouts = bottomUp(pendingLayouts);
        if (layouts.isEmpty()) {
            return new ComponentNode("root", ComponentTypes.ROOT, rootProps(), List.copyOf(innermostLevel));
        }
        List<ComponentNode> nodes = innermostLevel;
        for (int depth = layouts.size(); depth >= 1; depth--) {
            PendingLayout layout = layouts.get(depth - 1);
            ComponentNode partial =
                    new ComponentNode(layout.id(), layout.type(), layout.props(), List.copyOf(nodes));
            if (depth == 1) {
                List<ComponentNode> rootChildren = new ArrayList<>(framesBottomUp.getFirst().children());
                rootChildren.add(partial);
                return new ComponentNode("root", ComponentTypes.ROOT, rootProps(), List.copyOf(rootChildren));
            }
            Frame parentFrame = framesBottomUp.get(depth - 1);
            List<ComponentNode> parentChildren = new ArrayList<>(parentFrame.children());
            parentChildren.add(partial);
            nodes = parentChildren;
        }
        throw new IllegalStateException("unreachable");
    }

    private static <T> List<T> bottomUp(Deque<T> deque) {
        List<T> list = new ArrayList<>(deque);
        Collections.reverse(list);
        return list;
    }

    private void withinColumnScope(String columnKey, Frame frame, Runnable block) {
        paths.push(columnKey);
        counters.push(new HashMap<>());
        try {
            withinFrame(frame, block);
        } finally {
            counters.pop();
            paths.pop();
        }
    }

    private <T> T withinColumnScope(String columnKey, Frame frame, Supplier<T> block) {
        paths.push(columnKey);
        counters.push(new HashMap<>());
        try {
            return withinFrame(frame, block);
        } finally {
            counters.pop();
            paths.pop();
        }
    }

    private void withinFrame(Frame frame, Runnable block) {
        frames.push(frame);
        try {
            block.run();
        } finally {
            frames.pop();
        }
    }

    private <T> T withinFrame(Frame frame, Supplier<T> block) {
        frames.push(frame);
        try {
            return block.get();
        } finally {
            frames.pop();
        }
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

    /**
     * Column-scoped {@link Ui} that activates a dedicated frame and key path for each delegated
     * call so {@code cols[i]} can be used in any order within {@link #columns(int, Consumer)}.
     */
    private static final class ColumnScopedUi implements Ui {
        private final UiBinder parent;
        private final Frame frame;
        private final String columnKey;

        ColumnScopedUi(UiBinder parent, Frame frame, String columnKey) {
            this.parent = parent;
            this.frame = frame;
            this.columnKey = columnKey;
        }

        private void run(Runnable action) {
            parent.withinColumnScope(columnKey, frame, action);
        }

        private <T> T call(Supplier<T> action) {
            return parent.withinColumnScope(columnKey, frame, action);
        }

        @Override
        public void pageConfig(PageConfig config) {
            parent.pageConfig(config);
        }

        @Override
        public String path() {
            return parent.path();
        }

        @Override
        public void navigate(String routePath) {
            parent.navigate(routePath);
        }

        @Override
        public void title(String text) {
            run(() -> parent.title(text));
        }

        @Override
        public void markdown(String md) {
            run(() -> parent.markdown(md));
        }

        @Override
        public void text(String text) {
            run(() -> parent.text(text));
        }

        @Override
        public boolean button(String label) {
            return call(() -> parent.button(label));
        }

        @Override
        public String textInput(String label) {
            return call(() -> parent.textInput(label));
        }

        @Override
        public String chatInput() {
            return call(parent::chatInput);
        }

        @Override
        public void user(String message) {
            run(() -> parent.user(message));
        }

        @Override
        public void ai(String message) {
            run(() -> parent.ai(message));
        }

        @Override
        public String ai(TokenStream tokens) {
            return call(() -> parent.ai(tokens));
        }

        @Override
        public void code(String language, String source) {
            run(() -> parent.code(language, source));
        }

        @Override
        public void json(Object value) {
            run(() -> parent.json(value));
        }

        @Override
        public void table(List<Map<String, Object>> rows) {
            run(() -> parent.table(rows));
        }

        @Override
        public void image(String urlOrResource) {
            run(() -> parent.image(urlOrResource));
        }

        @Override
        public Optional<UploadedFile> fileUpload(String label) {
            return call(() -> parent.fileUpload(label));
        }

        @Override
        public void progress(double value) {
            run(() -> parent.progress(value));
        }

        @Override
        public StateStore state() {
            return parent.state();
        }

        @Override
        public <T> T withKey(String key, Function<Ui, T> block) {
            return call(() -> parent.withKey(key, block));
        }

        @Override
        public void container(Consumer<Ui> body) {
            run(() -> parent.container(body));
        }

        @Override
        public void columns(int n, Consumer<Ui[]> cols) {
            run(() -> parent.columns(n, cols));
        }

        @Override
        public void sidebar(Consumer<Ui> body) {
            run(() -> parent.sidebar(body));
        }

        @Override
        public boolean expander(String label, Consumer<Ui> body) {
            return call(() -> parent.expander(label, body));
        }
    }
}
