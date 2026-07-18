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
    private static final String SHUTDOWN_MESSAGE = "Session executor is shut down";

    private final LinkedBlockingQueue<QueuedTask<?>> queue = new LinkedBlockingQueue<>();
    private final Object lifecycleLock = new Object();
    private final Thread worker;
    private volatile boolean shutdown;

    SessionExecutor() {
        worker = Thread.ofVirtual().name("lumina-session-worker").unstarted(this::runLoop);
        worker.start();
    }

    <T> CompletableFuture<T> submit(Supplier<T> work) {
        QueuedTask<T> task = new QueuedTask<>(work);
        synchronized (lifecycleLock) {
            if (shutdown) {
                task.reject();
            } else {
                queue.add(task);
            }
        }
        return task.future();
    }

    void shutdown() {
        synchronized (lifecycleLock) {
            shutdown = true;
            QueuedTask<?> task;
            while ((task = queue.poll()) != null) {
                task.reject();
            }
        }
        worker.interrupt();
    }

    private void runLoop() {
        while (true) {
            QueuedTask<?> task;
            try {
                task = queue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            synchronized (lifecycleLock) {
                if (shutdown) {
                    task.reject();
                    return;
                }
            }
            task.run();
        }
    }

    private static final class QueuedTask<T> implements Runnable {
        private final Supplier<T> work;
        private final CompletableFuture<T> future = new CompletableFuture<>();

        private QueuedTask(Supplier<T> work) {
            this.work = work;
        }

        private CompletableFuture<T> future() {
            return future;
        }

        private void reject() {
            future.completeExceptionally(new IllegalStateException(SHUTDOWN_MESSAGE));
        }

        @Override
        public void run() {
            try {
                future.complete(work.get());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        }
    }
}
