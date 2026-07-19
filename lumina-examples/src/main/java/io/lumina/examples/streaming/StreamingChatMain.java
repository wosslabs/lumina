package io.lumina.examples.streaming;

import io.lumina.web.LuminaServer;

/**
 * Launches the Streaming Chat example on the default Lumina server port.
 */
public final class StreamingChatMain {
    private StreamingChatMain() {}

    /**
     * Starts the example server.
     *
     * @param args ignored command-line arguments
     */
    public static void main(String[] args) {
        var server = LuminaServer.start(new StreamingChatApp());
        System.out.println("Lumina Streaming Chat at " + server.uri());
    }
}
