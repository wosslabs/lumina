package io.lumina.ai;

final class EchoChatClient implements ChatClient {
    @Override
    public String prompt(String input) {
        return "Echo: " + input;
    }
}
