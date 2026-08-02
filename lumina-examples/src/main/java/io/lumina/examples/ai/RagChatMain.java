package io.lumina.examples.ai;

import io.lumina.web.LuminaServer;

/** Launches {@link RagChatApp}. */
public final class RagChatMain {
    private RagChatMain() {}

    public static void main(String[] args) {
        var server = LuminaServer.start(new RagChatApp());
        System.out.println("Lumina RAG chat at " + server.uri());
    }
}
