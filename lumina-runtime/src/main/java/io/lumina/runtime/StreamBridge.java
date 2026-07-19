package io.lumina.runtime;

import io.lumina.model.ComponentNode;
import java.util.List;

/**
 * Runtime hook the {@link UiBinder} calls while streaming an {@code ai_message}: it flushes an
 * interim structural patch (so the client has the target element) and emits text-only stream
 * frames (ADR-006). The default no-op bridge makes streaming behave like a normal append.
 */
interface StreamBridge {
    StreamBridge NOOP = new StreamBridge() {
        @Override public void flushBefore(List<ComponentNode> childrenSoFar) { }
        @Override public void streamStart(String nodeId) { }
        @Override public void streamAppend(String nodeId, String text) { }
        @Override public void streamEnd(String nodeId) { }
    };

    void flushBefore(List<ComponentNode> childrenSoFar);
    void streamStart(String nodeId);
    void streamAppend(String nodeId, String text);
    void streamEnd(String nodeId);
}
