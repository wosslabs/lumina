package io.lumina.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class SessionExecutorTest {
    @Test
    void submitAfterShutdownCompletesExceptionally() {
        SessionExecutor executor = new SessionExecutor();
        executor.shutdown();

        CompletableFuture<String> future = executor.submit(() -> "never");

        assertThatThrownBy(future::join)
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("Session executor is shut down");
    }

    @Test
    void shutdownCompletesQueuedFuturesExceptionally() throws InterruptedException {
        SessionExecutor executor = new SessionExecutor();
        CountDownLatch running = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CompletableFuture<String> first = executor.submit(() -> {
            running.countDown();
            awaitUninterruptibly(release);
            return "first";
        });
        assertThat(running.await(5, TimeUnit.SECONDS)).isTrue();
        CompletableFuture<String> queued = executor.submit(() -> "queued");

        executor.shutdown();

        try {
            assertThatThrownBy(() -> queued.orTimeout(1, TimeUnit.SECONDS).join())
                    .hasCauseInstanceOf(IllegalStateException.class)
                    .hasRootCauseMessage("Session executor is shut down");
        } finally {
            release.countDown();
        }
        assertThat(first.join()).isEqualTo("first");
    }

    private static void awaitUninterruptibly(CountDownLatch latch) {
        boolean interrupted = false;
        while (true) {
            try {
                latch.await();
                break;
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
