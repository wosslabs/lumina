package io.lumina.examples.layout;

import io.lumina.web.LuminaServer;

/**
 * Launches the layout demo example on the default Lumina server port.
 */
public final class LayoutDemoMain {
    private LayoutDemoMain() {}

    /**
     * Starts the example server.
     *
     * @param args ignored command-line arguments
     */
    public static void main(String[] args) {
        var server = LuminaServer.start(new LayoutDemoApp());
        System.out.println("Lumina Layout Demo at " + server.uri());
    }
}
