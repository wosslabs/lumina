package io.lumina.diff;

import io.lumina.model.ComponentNode;
import java.util.List;
import java.util.Map;

/**
 * One wire-level component tree mutation.
 *
 * @param op operation name
 * @param path component-tree path affected by the operation
 * @param node node payload for add and replace operations, otherwise {@code null}
 * @param props property payload for update operations, otherwise {@code null}
 * @param order child ids for reorder operations, otherwise {@code null}
 */
public record PatchOp(
        String op,
        String path,
        ComponentNode node,
        Map<String, Object> props,
        List<String> order
) {
    /**
     * Creates a patch operation and snapshots collection payloads.
     */
    public PatchOp {
        props = props == null ? null : Map.copyOf(props);
        order = order == null ? null : List.copyOf(order);
    }
}
