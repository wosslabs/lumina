package io.lumina.ai;

/** Factory for built-in {@link ChatClient} implementations. */
public final class ChatClients {
    private ChatClients() {}

    /** Offline stub that prefixes replies with {@code Echo: }. */
    public static ChatClient echo() {
        return new EchoChatClient();
    }
}
