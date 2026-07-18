package io.lumina.diff;

import io.lumina.model.ComponentNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Computes component tree patches using stable child ids as sibling keys.
 */
public final class TreeDiffer {
    /**
     * Creates a stateless tree differ.
     */
    public TreeDiffer() {}

    /**
     * Returns the operations that transform {@code before} into {@code after}.
     *
     * @param before previous component tree
     * @param after current component tree
     * @return immutable ordered patch operations
     */
    public List<PatchOp> diff(ComponentNode before, ComponentNode after) {
        List<PatchOp> operations = new ArrayList<>();
        diffNode(before, after, "", operations);
        return List.copyOf(operations);
    }

    private void diffNode(
            ComponentNode before,
            ComponentNode after,
            String path,
            List<PatchOp> operations) {
        if (!Objects.equals(before.id(), after.id())
                || !Objects.equals(before.type(), after.type())) {
            operations.add(new PatchOp("REPLACE", path, after, null, null));
            return;
        }

        if (!before.props().equals(after.props())) {
            operations.add(new PatchOp("UPDATE_PROPS", path, null, after.props(), null));
        }

        diffChildren(before.children(), after.children(), path, operations);
    }

    private void diffChildren(
            List<ComponentNode> beforeChildren,
            List<ComponentNode> afterChildren,
            String parentPath,
            List<PatchOp> operations) {
        Map<String, ComponentNode> beforeById = indexById(beforeChildren);
        Map<String, ComponentNode> afterById = indexById(afterChildren);

        for (int index = 0; index < beforeChildren.size(); index++) {
            ComponentNode child = beforeChildren.get(index);
            if (!afterById.containsKey(child.id())) {
                operations.add(new PatchOp(
                        "REMOVE", childPath(parentPath, index), null, null, null));
            }
        }

        for (int index = 0; index < afterChildren.size(); index++) {
            ComponentNode child = afterChildren.get(index);
            if (!beforeById.containsKey(child.id())) {
                operations.add(new PatchOp(
                        "ADD", childPath(parentPath, index), child, null, null));
            }
        }

        for (int index = 0; index < afterChildren.size(); index++) {
            ComponentNode afterChild = afterChildren.get(index);
            ComponentNode beforeChild = beforeById.get(afterChild.id());
            if (beforeChild != null) {
                diffNode(
                        beforeChild,
                        afterChild,
                        childPath(parentPath, index),
                        operations);
            }
        }

        List<String> beforeCommonOrder = commonOrder(beforeChildren, afterById);
        List<String> afterCommonOrder = commonOrder(afterChildren, beforeById);
        if (!beforeCommonOrder.equals(afterCommonOrder)) {
            operations.add(new PatchOp(
                    "REORDER",
                    parentPath,
                    null,
                    null,
                    afterChildren.stream().map(ComponentNode::id).toList()));
        }
    }

    private Map<String, ComponentNode> indexById(List<ComponentNode> children) {
        Map<String, ComponentNode> byId = new LinkedHashMap<>();
        for (ComponentNode child : children) {
            byId.put(child.id(), child);
        }
        return byId;
    }

    private List<String> commonOrder(
            List<ComponentNode> children,
            Map<String, ComponentNode> otherChildren) {
        return children.stream()
                .map(ComponentNode::id)
                .filter(otherChildren::containsKey)
                .toList();
    }

    private String childPath(String parentPath, int index) {
        return parentPath + "/children/" + index;
    }
}
