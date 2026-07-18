package io.lumina.runtime;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;

/**
 * Serial work queue for one session: submitted work runs one at a time, in submission order, on
 * a single dedicated virtual thread. This guarantees no two reruns for the same session ever
 * execute concurrently, while blocking work (e.g. a {@code ChatClient} call) does not tie up a
 * platform thread.
 */
final class SessionExecutor {
    private final LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    private final Thread worker;
    private volatile boolean shutdown;

    SessionExecutor() {
        worker = Thread.ofVirtual().name("lumina-session-worker").unstarted(this::runLoop);
        worker.start();
    }

    <T> CompletableFuture<T> submit(Supplier<T> work) {
        CompletableFuture<T> future = new CompletableFuture<>();
        queue.add(() -> {
            try {
                future.complete(work.get());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    void shutdown() {
        shutdown = true;
        worker.interrupt();
    }

    private void runLoop() {
        while (!shutdown) {
            try {
                queue.take().run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
