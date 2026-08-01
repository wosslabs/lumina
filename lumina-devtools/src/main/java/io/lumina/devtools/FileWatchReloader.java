package io.lumina.devtools;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Best-effort source-directory watcher for development reloads.
 */
public final class FileWatchReloader implements ReloadSpi, AutoCloseable {
    private final WatchService watchService;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public FileWatchReloader(Path directory) {
        Objects.requireNonNull(directory, "directory");
        try {
            Files.createDirectories(directory);
            this.watchService = FileSystems.getDefault().newWatchService();
            directory.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to watch " + directory, exception);
        }
    }

    @Override
    public void onChange(Runnable rebuild) {
        Objects.requireNonNull(rebuild, "rebuild");
        executor.submit(() -> {
            while (!executor.isShutdown()) {
                try {
                    WatchKey key = watchService.take();
                    if (!key.pollEvents().isEmpty()) rebuild.run();
                    key.reset();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
    }

    @Override
    public void close() {
        executor.shutdownNow();
        try {
            watchService.close();
        } catch (IOException ignored) {
            // Closing an already-closed watch service is harmless.
        }
    }
}
