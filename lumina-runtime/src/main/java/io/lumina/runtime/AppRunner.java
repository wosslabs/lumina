package io.lumina.runtime;

import io.lumina.LuminaApp;
import io.lumina.LuminaException;
import io.lumina.diff.PatchOp;
import io.lumina.diff.TreeDiffer;
import io.lumina.model.ComponentNode;
import io.lumina.session.internal.SessionState;
import io.lumina.session.internal.WidgetState;
import java.util.List;
import java.util.Objects;

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
        Objects.requireNonNull(app, "app");
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(intent, "intent");

        applyIntent(session.widgets(), intent);

        ComponentNode newRoot;
        try {
            UiBinder ui = new UiBinder(session);
            app.build(ui);
            newRoot = ui.buildRoot();
        } catch (Exception e) {
            if (previousRoot == null) {
                throw new LuminaException("Failed to build initial UI for intent '" + intent.name() + "'", e);
            }
            return RunResult.error(previousRoot, describe(e));
        }

        boolean firstRun = previousRoot == null;
        List<PatchOp> patches = firstRun ? List.of() : differ.diff(previousRoot, newRoot);
        previousRoot = newRoot;
        return firstRun ? RunResult.snapshot(newRoot) : RunResult.patched(newRoot, patches);
    }

    private void applyIntent(WidgetState widgets, Intent intent) {
        switch (intent.name()) {
            case "connect" -> { }
            case "click" -> widgets.set(requireTarget(intent), true);
            case "input" -> widgets.set(requireTarget(intent), intent.payload().get("value"));
            case "submit_chat" -> widgets.setChatSubmit(requireTarget(intent), (String) intent.payload().get("value"));
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
