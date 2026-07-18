package io.lumina.diff;

import io.lumina.model.ComponentNode;
import java.util.List;
import java.util.Map;

/**
 * One wire-level component tree mutation.
 */
public record PatchOp(
        String op,
        String path,
        ComponentNode node,
        Map<String, Object> props,
        List<String> order
) {
    public PatchOp {
        props = props == null ? null : Map.copyOf(props);
        order = order == null ? null : List.copyOf(order);
    }
}
