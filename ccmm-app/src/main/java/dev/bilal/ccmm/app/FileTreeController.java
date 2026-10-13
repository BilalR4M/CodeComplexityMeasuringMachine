package dev.bilal.ccmm.app;

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

    void close() {
        walker.shutdown();
    }
}
