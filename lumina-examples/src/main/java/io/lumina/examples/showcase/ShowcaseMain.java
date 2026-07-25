package io.lumina.examples.showcase;

import io.lumina.web.LuminaServer;

/**
 * Launches the P1.5 showcase demo on the default Lumina server port.
 */
public final class ShowcaseMain {
    private ShowcaseMain() {}

    /**
     * Starts the example server.
     *
     * @param args ignored command-line arguments
     */
    public static void main(String[] args) {
        var server = LuminaServer.start(new ShowcaseApp());
        System.out.println("Lumina Showcase at " + server.uri());
    }
}
