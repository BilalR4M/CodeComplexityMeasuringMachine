package dev.bilal.ccmm.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.bilal.ccmm.collab.protocol.Message;
import dev.bilal.ccmm.collab.protocol.TextOp;
import java.awt.GraphicsEnvironment;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import javafx.stage.Stage;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RemoteEditsTest {
    @TempDir
    Path root;

    @Test
    void aPathThatFailsTheProjectCheckIsLeftOut() throws Exception {
        Files.writeString(root.resolve("A.java"), "class A {}");
        String absolute = root.resolve("A.java").toAbsolutePath().toString();

        assertTrue(RemoteEdits.accepted(root, "A.java", List.of(new Message.Insert(absolute, 0, "X", 0, "eve"))).isEmpty());
        assertTrue(RemoteEdits.accepted(root, "A.java", List.of(new Message.Insert("../A.java", 0, "X", 0, "eve"))).isEmpty());
        assertTrue(RemoteEdits.accepted(root, "A.java", List.of(new Message.Insert("B.java", 0, "X", 0, "bob"))).isEmpty());

        List<TextOp> kept = RemoteEdits.accepted(root, "A.java", List.of(
                new Message.Insert("A.java", 0, "Y", 0, "bob"),
                new Message.Chat("Ada", "hi")));
        assertEquals(1, kept.size());
        assertEquals("Y", kept.get(0).text());
    }

    @Test
    void aSymlinkThatLeavesTheRootIsLeftOut() throws Exception {
        Path outside = Files.createTempDirectory("ccmm-remote-outside");
        try {
            Files.writeString(outside.resolve("Secret.java"), "class Secret {}");
            Path link = root.resolve("escape");
            if (!linkTo(link, outside)) {
                Assumptions.abort("cannot create a directory link");
            }
            List<TextOp> ops = RemoteEdits.accepted(root, "A.java", List.of(
                    new Message.Insert("escape/Secret.java", 0, "X", 0, "eve")));
            assertTrue(ops.isEmpty());
        } finally {
            Files.deleteIfExists(root.resolve("escape"));
            deleteTree(outside);
        }
    }

    @Test
    void oneFrameAppliesEveryRemoteOpAndUndoKeepsThem() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        Files.writeString(root.resolve("A.java"), "ab");

        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Runnable start = () -> {
            try {
                Platform.setImplicitExit(false);
                IdeWindow window = new IdeWindow();
                Stage stage = new Stage();
                stage.setScene(window.scene());
                window.setOnListed(() -> window.select(child(window, "A.java")));
                window.setOnOpened(() -> {
                    try {
                        window.editor().getUndoManager().forgetHistory();
                        window.editor().insertText(0, "X");
                        AtomicInteger batches = new AtomicInteger();
                        window.editor().multiPlainChanges().subscribe(changes -> batches.incrementAndGet());
                        window.applyRemote(List.of(new Message.Insert("A.java", 3, "Y", 0, "bob")));
                        window.applyRemote(List.of(
                                new Message.Insert("../A.java", 0, "NO", 0, "eve"),
                                new Message.Insert("A.java", 4, "Z", 0, "cara")));
                        Platform.runLater(() -> {
                            try {
                                assertEquals("XabYZ", window.editor().getText());
                                assertEquals(1, batches.get());
                                assertTrue(window.editor().getUndoManager().undo());
                                assertEquals("abYZ", window.editor().getText());
                                assertFalse(window.editor().getUndoManager().isUndoAvailable());
                                stage.close();
                                window.close();
                            } catch (Throwable thrown) {
                                failure.set(thrown);
                            } finally {
                                done.countDown();
                            }
                        });
                    } catch (Throwable thrown) {
                        failure.set(thrown);
                        done.countDown();
                    }
                });
                stage.show();
                window.open(root);
            } catch (Throwable thrown) {
                failure.set(thrown);
                done.countDown();
            }
        };
        try {
            Platform.startup(start);
        } catch (IllegalStateException alreadyRunning) {
            Platform.runLater(start);
        }

        assertTrue(done.await(15, TimeUnit.SECONDS));
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
    }

    private static TreeItem<FileNode> child(IdeWindow window, String name) {
        return window.listedRoot().getChildren().stream()
                .filter(item -> item.getValue().name().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private static boolean linkTo(Path link, Path target) throws Exception {
        try {
            Files.createSymbolicLink(link, target);
            return true;
        } catch (FileSystemException exception) {
            Process process = new ProcessBuilder("cmd", "/c", "mklink", "/J", link.toString(), target.toString())
                    .redirectErrorStream(true)
                    .start();
            process.waitFor();
            return Files.exists(link);
        }
    }

    private static void deleteTree(Path directory) throws Exception {
        if (!Files.exists(directory)) {
            return;
        }
        try (var walk = Files.walk(directory)) {
            walk.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (java.io.IOException ignored) {
                    // best-effort cleanup of the sibling directory
                }
            });
        }
    }
}
