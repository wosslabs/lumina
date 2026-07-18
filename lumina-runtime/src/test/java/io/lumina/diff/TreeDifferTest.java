package io.lumina.diff;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumina.model.ComponentNode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TreeDifferTest {
    @Test
    void appendChildProducesSingleAdd() {
        ComponentNode added = text("b");

        List<PatchOp> ops = new TreeDiffer().diff(root(title("a")), root(title("a"), added));

        assertThat(ops).containsExactly(
                new PatchOp("ADD", "/children/1", added, null, null));
    }

    @Test
    void identicalTreesProduceNoOps() {
        ComponentNode tree = root(title("a"));

        assertThat(new TreeDiffer().diff(tree, tree)).isEmpty();
    }

    @Test
    void changedNodeTypeProducesReplaceWithoutDescendantOps() {
        ComponentNode replacement = node("content", "text", Map.of("content", "new"));

        List<PatchOp> ops = new TreeDiffer().diff(
                root(node("content", "title", Map.of("content", "old"))),
                root(replacement));

        assertThat(ops).containsExactly(
                new PatchOp("REPLACE", "/children/0", replacement, null, null));
    }

    @Test
    void changedPropsProduceUpdateProps() {
        Map<String, Object> updatedProps = Map.of("content", "new");

        List<PatchOp> ops = new TreeDiffer().diff(
                root(node("content", "text", Map.of("content", "old"))),
                root(node("content", "text", updatedProps)));

        assertThat(ops).containsExactly(
                new PatchOp("UPDATE_PROPS", "/children/0", null, updatedProps, null));
    }

    @Test
    void removedChildProducesRemoveAtOriginalPath() {
        List<PatchOp> ops = new TreeDiffer().diff(
                root(title("a"), text("b")),
                root(text("b")));

        assertThat(ops).containsExactly(
                new PatchOp("REMOVE", "/children/0", null, null, null));
    }

    @Test
    void commonChildrenAreDiffedByIdRatherThanPosition() {
        ComponentNode before = root(
                node("a", "text", Map.of("content", "old a")),
                node("b", "text", Map.of("content", "old b")));
        ComponentNode after = root(
                node("b", "text", Map.of("content", "new b")),
                node("a", "text", Map.of("content", "old a")));

        List<PatchOp> ops = new TreeDiffer().diff(before, after);

        assertThat(ops).containsExactly(
                new PatchOp(
                        "UPDATE_PROPS",
                        "/children/0",
                        null,
                        Map.of("content", "new b"),
                        null),
                new PatchOp("REORDER", "", null, null, List.of("b", "a")));
    }

    @Test
    void nestedChangesUseJsonPointerLikePaths() {
        ComponentNode beforeContainer = container("panel", text("message"));
        ComponentNode added = node("action", "button", Map.of("label", "Retry"));
        ComponentNode afterContainer = container("panel", text("message"), added);

        List<PatchOp> ops = new TreeDiffer().diff(
                root(beforeContainer),
                root(afterContainer));

        assertThat(ops).containsExactly(
                new PatchOp("ADD", "/children/0/children/1", added, null, null));
    }

    private static ComponentNode root(ComponentNode... children) {
        return new ComponentNode("root", "root", Map.of(), List.of(children));
    }

    private static ComponentNode title(String id) {
        return node(id, "title", Map.of("content", id));
    }

    private static ComponentNode text(String id) {
        return node(id, "text", Map.of("content", id));
    }

    private static ComponentNode container(String id, ComponentNode... children) {
        return new ComponentNode(id, "container", Map.of(), List.of(children));
    }

    private static ComponentNode node(String id, String type, Map<String, Object> props) {
        return new ComponentNode(id, type, props, List.of());
    }
}
