package io.lumina.runtime;

import io.lumina.diff.PatchOp;
import io.lumina.model.ComponentNode;
import java.util.List;
import java.util.Objects;

/**
 * Result of one session run.
 *
 * @param root current component tree root, or the prior tree when this run errored
 * @param patches ops transforming the previously delivered tree into {@code root}; empty when
 *     {@code fullSnapshot} is set or when the run errored
 * @param fullSnapshot {@code true} when the caller must deliver the whole tree rather than {@code patches}
 * @param error user code failure message, or {@code null} on success
 */
public record RunResult(ComponentNode root, List<PatchOp> patches, boolean fullSnapshot, String error) {
    public RunResult {
        if (root == null && error == null) {
            throw new NullPointerException("root");
        }
        patches = List.copyOf(patches);
    }

    /**
     * Creates a successful result carrying only a root, with no patches and no error.
     *
     * @param root component tree root
     */
    public RunResult(ComponentNode root) {
        this(root, List.of(), false, null);
    }

    /**
     * Creates a full-snapshot result, used on session connect.
     *
     * @param root component tree root
     * @return snapshot result with no patches
     */
    public static RunResult snapshot(ComponentNode root) {
        return new RunResult(root, List.of(), true, null);
    }

    /**
     * Creates an incremental result carrying patches from the previous tree to {@code root}.
     *
     * @param root new component tree root
     * @param patches ops transforming the previous tree into {@code root}
     * @return patched result
     */
    public static RunResult patched(ComponentNode root, List<PatchOp> patches) {
        return new RunResult(root, patches, false, null);
    }

    /**
     * Creates an error result that keeps the previous tree and surfaces a failure message.
     *
     * @param previousRoot last known-good component tree root, or {@code null} before the first
     *     successful run
     * @param message failure message
     * @return error result with no patches
     */
    public static RunResult error(ComponentNode previousRoot, String message) {
        return new RunResult(previousRoot, List.of(), false, Objects.requireNonNull(message, "message"));
    }

    /**
     * Returns whether this run failed with a user code error.
     *
     * @return {@code true} if {@link #error()} is non-null
     */
    public boolean hasError() {
        return error != null;
    }
}
