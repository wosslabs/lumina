package io.lumina.examples.streaming;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lumina.web.LuminaServer;
import io.lumina.web.LuminaServerConfig;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class StreamingChatAppTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void chatSubmissionStreamsEchoReply() throws Exception {
        LuminaServer server = LuminaServer.start(
                new StreamingChatApp(), LuminaServerConfig.builder().port(0).build());
        try {
            CollectingListener listener = new CollectingListener();
            WebSocket webSocket = HttpClient.newHttpClient()
                    .newWebSocketBuilder()
                    .buildAsync(URI.create("ws://localhost:" + server.port() + "/ws"), listener)
                    .get(5, TimeUnit.SECONDS);
            String snapshot = listener.nextMessage();
            String chatId = findId(MAPPER.readTree(snapshot).path("root"), "chat_input");

            webSocket
                    .sendText(
                            "{\"type\":\"intent\",\"name\":\"submit_chat\",\"targetId\":\""
                                    + chatId
                                    + "\",\"payload\":{\"value\":\"ping\"}}",
                            true)
                    .join();

            List<String> messages = new ArrayList<>();
            boolean sawEnd = false;
            for (int i = 0; i < 50 && !sawEnd; i++) {
                String message = listener.nextMessage();
                messages.add(message);
                sawEnd = message.contains("\"op\":\"end\"");
            }

            assertThat(sawEnd).isTrue();
            assertThat(messages).anyMatch(m -> m.contains("\"op\":\"start\""));
            assertThat(messages).anyMatch(m -> m.contains("\"op\":\"append\""));

            String appended = messages.stream()
                    .filter(m -> m.contains("\"op\":\"append\""))
                    .map(StreamingChatAppTest::appendText)
                    .reduce("", String::concat);
            assertThat(appended).isEqualTo("Echo: ping");

            webSocket.abort();
        } finally {
            server.stop();
        }
    }

    private static String appendText(String message) {
        try {
            return MAPPER.readTree(message).path("text").asText();
        } catch (Exception e) {
            throw new RuntimeException(e);
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
