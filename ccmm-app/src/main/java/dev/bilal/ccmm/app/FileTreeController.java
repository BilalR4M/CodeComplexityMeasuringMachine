package dev.bilal.ccmm.app;

import dev.bilal.ccmm.app.document.LineEnding;
import dev.bilal.ccmm.app.document.Utf8Text;
import java.io.IOException;
import java.nio.charset.CharacterCodingException;
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

    /**
     * Reads {@code file} as strict UTF-8 off-thread. {@code onRead} receives
     * {@link FileRead#ok} or {@link FileRead#badUtf8}; IO failures yield
     * {@link FileRead#ioError}.
     */
    void read(Path file, Consumer<FileRead> onRead) {
        walker.execute(() -> {
            try {
                String text = Utf8Text.read(file);
                onRead.accept(FileRead.ok(text, LineEnding.detect(text)));
            } catch (CharacterCodingException badUtf8) {
                onRead.accept(FileRead.badUtf8());
            } catch (IOException exception) {
                onRead.accept(FileRead.ioError());
            }
        });
    }

    void close() {
        walker.shutdown();
    }

    /** Result of an off-thread file read. */
    sealed interface FileRead {
        record Ok(String text, LineEnding lineEnding) implements FileRead {}

        record BadUtf8() implements FileRead {}

        record IoError() implements FileRead {}

        static FileRead ok(String text, LineEnding ending) {
            return new Ok(text, ending);
        }

        static FileRead badUtf8() {
            return new BadUtf8();
        }

        static FileRead ioError() {
            return new IoError();
        }
    }
}
