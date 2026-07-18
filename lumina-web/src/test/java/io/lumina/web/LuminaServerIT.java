package io.lumina.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.util.Base64;
import java.util.Random;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Integration coverage for {@link LuminaServer}: an ephemeral-port ({@code 0}) embedded server
 * exercised end-to-end with the JDK's {@link HttpClient} and its WebSocket support, per ADR-005.
 */
class LuminaServerIT {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private LuminaServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void indexPageServesClientShell() throws Exception {
        server = LuminaServer.start(ui -> ui.title("Test App"), LuminaServerConfig.builder().port(0).build());

        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(server.uri()).GET().build(), HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("<lumina-app></lumina-app>")
                .contains("/lumina-web/lumina-client.js")
                .contains("/lumina-web/lumina.css");
    }

    /**
     * Manual acceptance: connect to {@code ws://host/ws}; render snapshot root children; apply
     * ADD, REMOVE, REPLACE, UPDATE_PROPS, and REORDER patches; submit chat as an ADR-003
     * {@code submit_chat} intent with the component target id and {@code payload.value}.
     */
    @Test
    void browserClientScriptIsServed() throws Exception {
        server = LuminaServer.start(ui -> ui.title("Test App"), LuminaServerConfig.builder().port(0).build());
        URI resource = server.uri().resolve("/lumina-web/lumina-client.js");

        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(resource).GET().build(), HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("customElements.define")
                .contains("\"submit_chat\"")
                .contains("\"file_upload\"")
                .contains("lumina-markdown")
                .contains("lumina-button")
                .contains("lumina-text-input")
                .contains("lumina-code")
                .contains("lumina-json")
                .contains("lumina-table")
                .contains("lumina-image")
                .contains("lumina-file-upload")
                .contains("lumina-progress");
    }

    @Test
    void websocketReceivesSnapshotOnConnect() throws Exception {
        server = LuminaServer.start(ui -> ui.title("T"), LuminaServerConfig.builder().port(0).build());
        CollectingListener listener = new CollectingListener();
        WebSocket webSocket = openWebSocket(listener);

        String message = listener.nextMessage();

        assertThat(message).contains("\"type\":\"snapshot\"");
        assertThat(message).contains("\"content\":\"T\"");
        webSocket.abort();
    }

    @Test
    void websocketIntentProducesPatch() throws Exception {
        server = LuminaServer.start(
                ui -> {
                    if (ui.button("Add")) {
                        ui.text("added");
                    }
                },
                LuminaServerConfig.builder().port(0).build());
        CollectingListener listener = new CollectingListener();
        WebSocket webSocket = openWebSocket(listener);
        String snapshot = listener.nextMessage();
        String buttonId = findId(MAPPER.readTree(snapshot).path("root"), "button");

        webSocket.sendText(
                "{\"type\":\"intent\",\"name\":\"click\",\"targetId\":\"" + buttonId + "\",\"payload\":{}}", true);
        String patch = listener.nextMessage();

        assertThat(patch).contains("\"type\":\"patch\"");
        assertThat(patch).contains("\"content\":\"added\"");
        webSocket.abort();
    }

    @Test
    void websocketMalformedIntentProducesError() throws Exception {
        server = LuminaServer.start(ui -> ui.title("T"), LuminaServerConfig.builder().port(0).build());
        CollectingListener listener = new CollectingListener();
        WebSocket webSocket = openWebSocket(listener);
        listener.nextMessage();

        webSocket.sendText("not json", true);
        String error = listener.nextMessage();

        assertThat(error).isEqualTo("{\"type\":\"error\",\"message\":\"Invalid message\"}");
        assertThat(error).doesNotContain("Malformed intent message", "LuminaException");
        webSocket.abort();
    }

    @Test
    void websocketApplicationFailureUsesStablePublicError() throws Exception {
        server = LuminaServer.start(
                ui -> {
                    ui.title("T");
                    if (ui.button("Fail")) {
                        throw new IllegalStateException("secret application detail");
                    }
                },
                LuminaServerConfig.builder().port(0).build());
        CollectingListener listener = new CollectingListener();
        WebSocket webSocket = openWebSocket(listener);
        String snapshot = listener.nextMessage();
        String buttonId = findId(MAPPER.readTree(snapshot).path("root"), "button");

        webSocket.sendText(
                "{\"type\":\"intent\",\"name\":\"click\",\"targetId\":\"" + buttonId + "\",\"payload\":{}}", true);
        String error = listener.nextMessage();

        assertThat(error).isEqualTo("{\"type\":\"error\",\"message\":\"Application error\"}");
        assertThat(error).doesNotContain("secret application detail", "IllegalStateException");
        webSocket.abort();
    }

    @Test
    void websocketUploadLargerThan64KbRoundTripsToApplication() throws Exception {
        server = LuminaServer.start(
                ui -> ui.fileUpload("Attachment")
                        .ifPresent(file -> ui.text("uploaded:" + file.fileName() + ":" + file.bytes().length)),
                LuminaServerConfig.builder().port(0).build());
        CollectingListener listener = new CollectingListener();
        WebSocket webSocket = openWebSocket(listener);
        String snapshot = listener.nextMessage();
        String uploadId = findId(MAPPER.readTree(snapshot).path("root"), "file_upload");

        byte[] fileBytes = new byte[100 * 1024];
        new Random(42).nextBytes(fileBytes);
        String data = Base64.getEncoder().encodeToString(fileBytes);
        webSocket.sendText(
                "{\"type\":\"intent\",\"name\":\"file_upload\",\"targetId\":\"" + uploadId + "\","
                        + "\"payload\":{\"fileName\":\"photo.bin\",\"contentType\":\"application/octet-stream\","
                        + "\"data\":\"" + data + "\"}}",
                true);
        String patch = listener.nextMessage();

        assertThat(patch).contains("\"type\":\"patch\"");
        assertThat(patch).contains("\"content\":\"uploaded:photo.bin:" + fileBytes.length + "\"");
        webSocket.abort();
    }

    @Test
    void websocketRejectsCrossSiteOrigin() {
        server = LuminaServer.start(ui -> ui.title("T"), LuminaServerConfig.builder().port(0).build());
        URI wsUri = URI.create("ws://127.0.0.1:" + server.port() + "/ws");

        assertThatThrownBy(() -> HttpClient.newHttpClient()
                        .newWebSocketBuilder()
                        .header("Origin", "http://evil.example")
                        .buildAsync(wsUri, new CollectingListener())
                        .get(5, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class);
    }

    @Test
    void websocketAcceptsLocalhostOrigin() throws Exception {
        server = LuminaServer.start(ui -> ui.title("T"), LuminaServerConfig.builder().port(0).build());
        URI wsUri = URI.create("ws://127.0.0.1:" + server.port() + "/ws");
        CollectingListener listener = new CollectingListener();

        WebSocket webSocket = HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .header("Origin", "http://127.0.0.1:" + server.port())
                .buildAsync(wsUri, listener)
                .get(5, TimeUnit.SECONDS);

        assertThat(listener.nextMessage()).contains("\"type\":\"snapshot\"");
        webSocket.abort();
    }

    @Test
    void websocketRejectsConnectionsBeyondSessionCap() throws Exception {
        server = LuminaServer.start(
                ui -> ui.title("T"), LuminaServerConfig.builder().port(0).maxSessions(1).build());
        CollectingListener firstListener = new CollectingListener();
        WebSocket first = openWebSocket(firstListener);
        firstListener.nextMessage();

        assertThatThrownBy(() -> openWebSocket(new CollectingListener())).isInstanceOf(ExecutionException.class);

        first.abort();
    }

    private WebSocket openWebSocket(CollectingListener listener) throws Exception {
        URI wsUri = URI.create("ws://127.0.0.1:" + server.port() + "/ws");
        return HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(wsUri, listener)
                .get(5, TimeUnit.SECONDS);
    }

    private static String findId(JsonNode node, String type) {
        if (type.equals(node.path("type").asText())) {
            return node.path("id").asText();
        }
        for (JsonNode child : node.path("children")) {
            String found = findId(child, type);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /** Collects complete WebSocket text messages, re-requesting after each per JDK contract. */
    private static final class CollectingListener implements WebSocket.Listener {
        private final LinkedBlockingQueue<String> messages = new LinkedBlockingQueue<>();
        private final StringBuilder buffer = new StringBuilder();

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                messages.offer(buffer.toString());
                buffer.setLength(0);
            }
            webSocket.request(1);
            return null;
        }

        String nextMessage() throws InterruptedException {
            String message = messages.poll(5, TimeUnit.SECONDS);
            if (message == null) {
                throw new AssertionError("Timed out waiting for a WebSocket message");
            }
            return message;
        }
    }
}
