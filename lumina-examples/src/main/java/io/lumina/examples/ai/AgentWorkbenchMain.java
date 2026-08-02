package io.lumina.examples.ai;

import io.lumina.web.LuminaServer;

/** Launches {@link AgentWorkbenchApp}. */
public final class AgentWorkbenchMain {
    private AgentWorkbenchMain() {}

    public static void main(String[] args) {
        var server = LuminaServer.start(new AgentWorkbenchApp());
        System.out.println("Lumina Agent workbench at " + server.uri());
    }
}
