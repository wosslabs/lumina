package io.lumina.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lumina.LuminaException;
import io.lumina.diff.PatchOp;
import io.lumina.model.ComponentNode;
import io.lumina.runtime.Intent;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Encodes server-to-browser messages and decodes browser-to-server intents per ADR-003, using
 * Jackson's native record support so {@link ComponentNode} and {@link PatchOp} need no
 * hand-written (de)serializers.
 */
public final class ProtocolCodec {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ProtocolCodec() {}

    /**
     * Encodes a full component tree as a {@code snapshot} message.
     *
     * @param root component tree root
     * @return JSON snapshot message
     */
    public static String toSnapshotJson(ComponentNode root) {
        Objects.requireNonNull(root, "root");
        return write(new SnapshotMessage("snapshot", root));
    }

    /**
     * Encodes a batch of tree mutations as a {@code patch} message.
     *
     * @param ops ops transforming the previous snapshot into the current tree
     * @return JSON patch message
     */
    public static String toPatchJson(List<PatchOp> ops) {
        Objects.requireNonNull(ops, "ops");
        return write(new PatchMessage("patch", ops));
    }

    /**
     * Encodes a failure message that does not expose framework internals.
     *
     * @param message failure description shown to the browser
     * @return JSON error message
     */
    public static String toErrorJson(String message) {
        Objects.requireNonNull(message, "message");
        return write(new ErrorMessage("error", message));
    }

    /**
     * Decodes a browser-originated {@code intent} message.
     *
     * @param json raw WebSocket text message
     * @return decoded intent
     * @throws LuminaException if {@code json} is not a valid intent message
     */
    public static Intent parseIntent(String json) {
        Objects.requireNonNull(json, "json");
        IntentMessage message;
        try {
            message = MAPPER.readValue(json, IntentMessage.class);
        } catch (JsonProcessingException e) {
            throw new LuminaException("Malformed intent message", e);
        }
        if (!"intent".equals(message.type())) {
            throw new LuminaException("Message type must be 'intent'");
        }
        if (message.name() == null) {
            throw new LuminaException("Intent message missing 'name'");
        }
        Map<String, Object> payload = message.payload() == null ? Map.of() : message.payload();
        return new Intent(message.name(), message.targetId(), payload);
    }

    private static String write(Object message) {
        try {
            return MAPPER.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new LuminaException("Failed to encode protocol message", e);
        }
    }

    private record SnapshotMessage(String type, ComponentNode root) {}

    private record PatchMessage(String type, List<PatchOp> ops) {}

    private record ErrorMessage(String type, String message) {}

    private record IntentMessage(
            String type, String sessionId, String name, String targetId, Map<String, Object> payload) {}
}
