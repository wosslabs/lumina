package io.lumina.examples.helloai;

import io.lumina.web.LuminaServer;

/**
 * Launches the Hello AI example on the default Lumina server port.
 */
public final class HelloAiMain {
    private HelloAiMain() {}

    /**
     * Starts the example server.
     *
     * @param args ignored command-line arguments
     */
    public static void main(String[] args) {
        var server = LuminaServer.start(new HelloAiApp());
        System.out.println("Lumina Hello AI at " + server.uri());
    }
}
