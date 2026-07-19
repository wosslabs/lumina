package io.lumina.ai;

final class EchoChatClient implements ChatClient {
    @Override
    public String prompt(String input) {
        return "Echo: " + input;
    }

    @Override
    public TokenStream stream(String input) {
        String reply = prompt(input);
        java.util.List<String> chunks = new java.util.ArrayList<>();
        String[] words = reply.split(" ", -1);
        for (int i = 0; i < words.length; i++) {
            chunks.add(i < words.length - 1 ? words[i] + " " : words[i]);
        }
        return TokenStream.fromIterable(chunks);
    }
}
