package io.lumina.runtime;

import io.lumina.LuminaApp;
import io.lumina.LuminaException;
import io.lumina.diff.PatchOp;
import io.lumina.diff.TreeDiffer;
import io.lumina.model.ComponentNode;
import io.lumina.model.ComponentTypes;
import io.lumina.session.internal.SessionState;
import io.lumina.session.internal.WidgetState;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Drives one session's rerun loop: applies an {@link Intent} to widget state, rebuilds the
 * component tree via {@link UiBinder}, and diffs it against the previous run.
 *
 * <p>Instances are stateful (they retain the previous tree) and must be confined to a single
 * session; {@link SessionHandle} owns one instance per session and only ever calls it from its
 * {@link SessionExecutor} thread.
 */
final class AppRunner {
    private final TreeDiffer differ = new TreeDiffer();
    private ComponentNode previousRoot;

    /**
     * Applies {@code intent} to {@code session}'s widget state, rebuilds the app's UI, and
     * returns the resulting root plus patches (or a full snapshot on the first successful run).
     *
     * @param app application entry point
     * @param session session-scoped state shared across reruns
     * @param intent intent to apply before rebuilding
     * @return run result carrying the new tree, patches, or an error
     */
    RunResult run(LuminaApp app, SessionState session, Intent intent) {
        return run(app, session, intent, RunSink.NOOP);
    }

    /**
     * Same as {@link #run(LuminaApp, SessionState, Intent)}, but streams interim structural
     * flushes and text {@code stream} frames to {@code sink} as the app builds, and suppresses
     * the final patch ops that would otherwise redundantly resend content already streamed.
     *
     * @param app application entry point
     * @param session session-scoped state shared across reruns
     * @param intent intent to apply before rebuilding
     * @param sink receiver of interim results and stream frames
     * @return run result carrying the new tree, patches, or an error
     */
    RunResult run(LuminaApp app, SessionState session, Intent intent, RunSink sink) {
        Objects.requireNonNull(app, "app");
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(intent, "intent");
        Objects.requireNonNull(sink, "sink");

        applyIntent(session.widgets(), intent);

        StreamBridge bridge = new StreamBridge() {
            @Override
            public void flushBefore(ComponentNode interimRoot) {
                sink.deliverInterim(previousRoot == null
                        ? RunResult.snapshot(interimRoot)
                        : RunResult.patched(interimRoot, differ.diff(previousRoot, interimRoot)));
                previousRoot = interimRoot;
            }

            @Override
            public void streamStart(String nodeId) {
                sink.sendFrame(StreamFrames.start(nodeId));
            }

            @Override
            public void streamAppend(String nodeId, String text) {
                sink.sendFrame(StreamFrames.append(nodeId, text));
            }

            @Override
            public void streamEnd(String nodeId) {
                sink.sendFrame(StreamFrames.end(nodeId));
            }
        };

        ComponentNode newRoot;
        UiBinder ui;
        try {
            ui = new UiBinder(session, bridge);
            app.build(ui);
            newRoot = ui.buildRoot();
        } catch (Exception e) {
            return RunResult.error(previousRoot, describe(e));
        }

        boolean firstRun = previousRoot == null;
        List<PatchOp> raw = firstRun ? List.of() : differ.diff(previousRoot, newRoot);
        Set<String> streamed = ui.streamedNodeIds();
        List<PatchOp> filtered = raw.stream()
                .filter(op -> !isSuppressed(op, newRoot, streamed))
                .toList();
        previousRoot = newRoot;
        return firstRun ? RunResult.snapshot(newRoot) : RunResult.patched(newRoot, filtered);
    }

    private boolean isSuppressed(PatchOp op, ComponentNode root, Set<String> streamedIds) {
        if (!"UPDATE_PROPS".equals(op.op())) {
            return false;
        }
        String nodeId = nodeIdAtPath(root, op.path());
        return nodeId != null && streamedIds.contains(nodeId);
    }

    /**
     * Resolves the id of the node addressed by a flat {@code /children/<index>} style path,
     * walking one {@code children}/{@code index} pair at a time from {@code root}.
     *
     * @param root tree to resolve the path against
     * @param path patch op path, e.g. {@code "/children/2"}
     * @return the resolved node's id, or {@code null} if the path can't be resolved
     */
    private String nodeIdAtPath(ComponentNode root, String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        String[] segments = path.split("/");
        ComponentNode current = root;
        boolean descended = false;
        int i = 0;
        while (i < segments.length) {
            String segment = segments[i];
            if (segment.isEmpty()) {
                i++;
                continue;
            }
            if (!"children".equals(segment) || i + 1 >= segments.length) {
                return null;
            }
            int index;
            try {
                index = Integer.parseInt(segments[i + 1]);
            } catch (NumberFormatException e) {
                return null;
            }
            List<ComponentNode> kids = current.children();
            if (index < 0 || index >= kids.size()) {
                return null;
            }
            current = kids.get(index);
            descended = true;
            i += 2;
        }
        return descended ? current.id() : null;
    }

    private void applyIntent(WidgetState widgets, Intent intent) {
        switch (intent.name()) {
            case "connect" -> { }
            case "click" -> widgets.set(requireTarget(intent), true);
            case "input" -> widgets.set(requireTarget(intent), intent.payload().get("value"));
            case "submit_chat" -> widgets.setChatSubmit(requireTarget(intent), (String) intent.payload().get("value"));
            case "file_upload" -> widgets.set(requireTarget(intent), intent.payload().get("file"));
            case "expander_toggle" -> {
                String key = requireTarget(intent);
                boolean open = Boolean.TRUE.equals(widgets.value(key));
                widgets.set(key, !open);
            }
            default -> throw new LuminaException("Unknown intent: " + intent.name());
        }
    }

    private String requireTarget(Intent intent) {
        String targetId = intent.targetId();
        if (targetId == null) {
            throw new LuminaException("Intent '" + intent.name() + "' requires a targetId");
        }
        return targetId;
    }

    private String describe(Exception e) {
        String message = e.getMessage();
        return message != null ? message : e.getClass().getSimpleName();
    }
}
