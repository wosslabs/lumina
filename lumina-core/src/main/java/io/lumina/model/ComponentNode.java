package io.lumina.model;

import java.util.List;
import java.util.Map;

/**
 * Immutable node in the server-side component tree.
 *
 * @param id stable node id
 * @param type component type constant
 * @param props serializable properties
 * @param children ordered children
 */
public record ComponentNode(
        String id,
        String type,
        Map<String, Object> props,
        List<ComponentNode> children
) {
    public ComponentNode {
        props = Map.copyOf(props);
        children = List.copyOf(children);
    }
}
