package io.lumina.examples.helloai;

import io.lumina.web.LuminaServer;

public final class HelloAiMain {
    public static void main(String[] args) {
        var server = LuminaServer.start(new HelloAiApp());
        System.out.println("Lumina Hello AI at " + server.uri());
    }
}
