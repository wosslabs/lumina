package io.lumina.examples.helloai;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lumina.web.LuminaServer;
import io.lumina.web.LuminaServerConfig;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class HelloAiAppTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void chatSubmissionProducesEchoPatch() throws Exception {
        LuminaServer server =
                LuminaServer.start(new HelloAiApp(), LuminaServerConfig.builder().port(0).build());
        try {
            CollectingListener listener = new CollectingListener();
            WebSocket webSocket = HttpClient.newHttpClient()
                    .newWebSocketBuilder()
                    .buildAsync(URI.create("ws://localhost:" + server.port() + "/ws"), listener)
                    .get(5, TimeUnit.SECONDS);
            webSocket.sendText("{\"type\":\"intent\",\"name\":\"connect\",\"payload\":{\"path\":\"/\"}}", true);
            String snapshot = listener.nextMessage();
            String chatId = findId(MAPPER.readTree(snapshot).path("root"), "chat_input");

            webSocket
                    .sendText(
                            "{\"type\":\"intent\",\"name\":\"submit_chat\",\"targetId\":\""
                                    + chatId
                                    + "\",\"payload\":{\"value\":\"ping\"}}",
                            true)
                    .join();

            assertThat(listener.nextMessage()).contains("\"type\":\"patch\"", "Echo: ping");
            webSocket.abort();
        } finally {
            server.stop();
        }
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
                throw new AssertionError("Timed out waiting for WebSocket message");
            }
            return message;
        }
    }
}
