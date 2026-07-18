package io.lumina.ai;

/** Factory for built-in {@link ChatClient} implementations. */
public final class ChatClients {
    private ChatClients() {}

    /**
     * Offline stub that prefixes replies with {@code Echo: }.
     *
     * @return echo {@link ChatClient} for tests and local development
     */
    public static ChatClient echo() {
        return new EchoChatClient();
    }
}
