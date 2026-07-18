package io.lumina.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumina.LuminaException;
import io.lumina.diff.PatchOp;
import io.lumina.model.ComponentNode;
import io.lumina.runtime.Intent;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProtocolCodecTest {
    @Test
    void encodesSnapshot() {
        String json = ProtocolCodec.toSnapshotJson(sampleRoot());

        assertThat(json).contains("\"type\":\"snapshot\"");
        assertThat(json).contains("\"id\":\"root\"");
    }

    @Test
    void encodesPatch() {
        ComponentNode added = node("message-2", "text", Map.of("content", "Hello"));
        PatchOp addOp = new PatchOp("ADD", "/children/1", added, null, null);

        String json = ProtocolCodec.toPatchJson(List.of(addOp));

        assertThat(json).contains("\"type\":\"patch\"");
        assertThat(json).contains("\"op\":\"ADD\"");
        assertThat(json).contains("\"path\":\"/children/1\"");
    }

    @Test
    void encodesError() {
        String json = ProtocolCodec.toErrorJson("Unable to process intent");

        assertThat(json).isEqualTo("{\"type\":\"error\",\"message\":\"Unable to process intent\"}");
    }

    @Test
    void parsesConnectIntentWithoutTargetOrPayload() {
        Intent intent = ProtocolCodec.parseIntent("{\"type\":\"intent\",\"name\":\"connect\"}");

        assertThat(intent).isEqualTo(Intent.connect());
    }

    @Test
    void parsesClickIntentWithTarget() {
        String json = "{\"type\":\"intent\",\"name\":\"click\",\"targetId\":\"auto:/button#0\",\"payload\":{}}";

        Intent intent = ProtocolCodec.parseIntent(json);

        assertThat(intent).isEqualTo(Intent.click("auto:/button#0"));
    }

    @Test
    void parsesInputIntentWithPayloadValue() {
        String json = "{\"type\":\"intent\",\"name\":\"input\",\"targetId\":\"auto:/text_input#0\","
                + "\"payload\":{\"value\":\"hello\"}}";

        Intent intent = ProtocolCodec.parseIntent(json);

        assertThat(intent).isEqualTo(Intent.input("auto:/text_input#0", "hello"));
    }

    @Test
    void malformedJsonThrowsLuminaException() {
        assertThatThrownBy(() -> ProtocolCodec.parseIntent("not json"))
                .isInstanceOf(LuminaException.class);
    }

    @Test
    void missingTypeThrowsLuminaException() {
        assertThatThrownBy(() -> ProtocolCodec.parseIntent("{\"name\":\"connect\"}"))
                .isInstanceOf(LuminaException.class);
    }

    @Test
    void wrongTypeThrowsLuminaException() {
        assertThatThrownBy(() -> ProtocolCodec.parseIntent("{\"type\":\"snapshot\",\"name\":\"connect\"}"))
                .isInstanceOf(LuminaException.class);
    }

    @Test
    void missingNameThrowsLuminaException() {
        assertThatThrownBy(() -> ProtocolCodec.parseIntent("{\"type\":\"intent\"}"))
                .isInstanceOf(LuminaException.class);
    }

    private static ComponentNode sampleRoot() {
        return new ComponentNode("root", "root", Map.of(), List.of());
    }

    private static ComponentNode node(String id, String type, Map<String, Object> props) {
        return new ComponentNode(id, type, props, List.of());
    }
}
