package io.lumina.examples.ai;

import io.lumina.web.LuminaServer;

/** Launches {@link McpConsoleApp}. */
public final class McpConsoleMain {
    private McpConsoleMain() {}

    public static void main(String[] args) {
        var server = LuminaServer.start(new McpConsoleApp());
        System.out.println("Lumina MCP console at " + server.uri());
    }
}
