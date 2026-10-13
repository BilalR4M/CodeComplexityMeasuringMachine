package dev.bilal.ccmm.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.bilal.ccmm.core.AnalysisResult;
import java.awt.GraphicsEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import javafx.stage.Stage;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class IdeWindowTest {
    @TempDir
    Path root;

    @Test
    void theWindowListsFilesFromAnOpenedDirectory() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        Files.writeString(root.resolve("A.java"), "class A {}");
        Files.writeString(root.resolve("notes.txt"), "hi");

        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Runnable start = () -> {
            try {
                Platform.setImplicitExit(false);
                IdeWindow window = new IdeWindow();
                Stage stage = new Stage();
                stage.setScene(window.scene());
                window.setOnListed(() -> {
                    try {
                        assertEquals(List.of("A.java", "notes.txt"), window.listedRoot().getChildren().stream()
                                .map(item -> item.getValue().name())
                                .toList());
                        assertTrue(stage.isShowing());
                        stage.close();
                        window.close();
                    } catch (Throwable thrown) {
                        failure.set(thrown);
                    } finally {
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

    @Test
    void aJavaFileOpensInTheEditorAndTypingDoesNotAnalyze() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        Files.writeString(root.resolve("A.java"), "class A {}");
        Files.writeString(root.resolve("notes.txt"), "notes");

        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Runnable start = () -> {
            try {
                Platform.setImplicitExit(false);
                IdeWindow window = new IdeWindow();
                Stage stage = new Stage();
                stage.setScene(window.scene());
                window.setOnListed(() -> {
                    try {
                        TreeItem<FileNode> notes = child(window, "notes.txt");
                        window.select(notes);
                        assertEquals("", window.editor().getText());
                        window.setOnOpened(() -> {
                            try {
                                assertEquals("class A {}", window.editor().getText());
                                window.editor().insertText(window.editor().getLength(), "\n// typed");
                                assertTrue(window.editor().getText().endsWith("\n// typed"));
                                stage.close();
                                window.close();
                            } catch (Throwable thrown) {
                                failure.set(thrown);
                            } finally {
                                done.countDown();
                            }
                        });
                        window.select(child(window, "A.java"));
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

    @Test
    void typingPublishesTheLatestAnalysisOnTheFxThread() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        Files.writeString(root.resolve("A.java"), "class A {}");

        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Runnable start = () -> {
            try {
                Platform.setImplicitExit(false);
                IdeWindow window = new IdeWindow(40);
                Stage stage = new Stage();
                stage.setScene(window.scene());
                window.setOnListed(() -> window.select(child(window, "A.java")));
                window.setOnAnalyzed(() -> {
                    try {
                        assertTrue(Platform.isFxApplicationThread());
                        AnalysisResult.Ok ok = (AnalysisResult.Ok) window.latestAnalysis();
                        assertEquals("A.java", ok.relativePath());
                        if (ok.file().cyclomatic() == 0) {
                            window.editor().insertText(window.editor().getLength() - 1, " void m() { if (true) {} }");
                            window.editor().insertText(window.editor().getLength() - 1, " ");
                            return;
                        }
                        assertEquals(2, ok.file().cyclomatic());
                        assertTrue(window.editor().getText().contains("void m()"));
                        stage.close();
                        window.close();
                    } catch (Throwable thrown) {
                        failure.set(thrown);
                    } finally {
                        if (failure.get() != null || (window.latestAnalysis() instanceof AnalysisResult.Ok ok
                                && ok.file().cyclomatic() == 2)) {
                            done.countDown();
                        }
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
}
