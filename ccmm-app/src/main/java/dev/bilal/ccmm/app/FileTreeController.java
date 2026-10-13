package dev.bilal.ccmm.app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

final class FileTreeController {
    private final ExecutorService walker = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "ccmm-file-tree");
        thread.setDaemon(true);
        return thread;
    });

    void open(Path directory, Consumer<FileNode> onWalked) {
        walker.execute(() -> onWalked.accept(FileNode.tree(directory)));
    }

    void read(Path file, Consumer<String> onRead) {
        walker.execute(() -> {
            try {
                onRead.accept(Files.readString(file));
            } catch (IOException exception) {
                onRead.accept(null);
            }
        });
    }

    void close() {
        walker.shutdown();
    }
}
