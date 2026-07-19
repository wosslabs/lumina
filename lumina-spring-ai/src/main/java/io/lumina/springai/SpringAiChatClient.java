package io.lumina.springai;

import io.lumina.LuminaException;
import io.lumina.ai.TokenStream;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;

/**
 * Adapts a Spring AI {@link ChatClient} to Lumina's {@link io.lumina.ai.ChatClient} SPI, bridging
 * Spring AI's reactive {@link Flux} streaming to Lumina's blocking {@link TokenStream}.
 */
public class SpringAiChatClient implements io.lumina.ai.ChatClient {

    private final ChatClient chatClient;

    /**
     * Creates an adapter wrapping the given Spring AI chat client.
     *
     * @param chatClient the Spring AI fluent chat client; must not be null
     */
    public SpringAiChatClient(ChatClient chatClient) {
        this.chatClient = Objects.requireNonNull(chatClient, "chatClient");
    }

    @Override
    public String prompt(String input) {
        return chatClient.prompt().user(input).call().content();
    }

    @Override
    public TokenStream stream(String input) {
        return toTokenStream(chatClient.prompt().user(input).stream().content());
    }

    /**
     * Bridges a Spring AI streaming {@link Flux} of text chunks to a blocking {@link TokenStream}.
     *
     * <p>The flux is subscribed to eagerly; chunks are buffered on a queue and drained by the
     * returned stream's iterator, which blocks until the next chunk, the end-of-stream sentinel,
     * or an error is available.
     *
     * @param flux the upstream chunk source; must not be null
     * @return a token stream yielding the flux's chunks in order
     */
    static TokenStream toTokenStream(Flux<String> flux) {
        Objects.requireNonNull(flux, "flux");
        BlockingQueue<Object> queue = new LinkedBlockingQueue<>();
        Object end = new Object();
        flux.subscribe(queue::add, error -> queue.add(new Err(error)), () -> queue.add(end));
        return () -> new Iterator<>() {
            String buffered;
            boolean done;
            boolean hasBuffered;

            @Override
            public boolean hasNext() {
                if (hasBuffered) {
                    return true;
                }
                if (done) {
                    return false;
                }
                Object item = take(queue);
                if (item == end) {
                    done = true;
                    return false;
                }
                if (item instanceof Err err) {
                    done = true;
                    throw new LuminaException("Spring AI stream failed", err.cause);
                }
                buffered = (String) item;
                hasBuffered = true;
                return true;
            }

            @Override
            public String next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                hasBuffered = false;
                String value = buffered;
                buffered = null;
                return value;
            }
        };
    }

    private static Object take(BlockingQueue<Object> queue) {
        try {
            return queue.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LuminaException("Interrupted while waiting for Spring AI stream", e);
        }
    }

    private static final class Err {
        private final Throwable cause;

        private Err(Throwable cause) {
            this.cause = cause;
        }
    }
}
