package dev.bilal.ccmm.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.bilal.ccmm.app.document.Document;
import dev.bilal.ccmm.app.document.LineEnding;
import dev.bilal.ccmm.app.editor.SaveClosePrompt;
import dev.bilal.ccmm.app.session.SessionStore;
import dev.bilal.ccmm.app.settings.SettingsStore;
import dev.bilal.ccmm.app.workbench.DirectoryPrompt;
import dev.bilal.ccmm.core.Analyzer;
import java.awt.GraphicsEnvironment;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

class LargeFileModeTest {
    @TempDir
    Path root;

    @TempDir
    Path config;

    @Test
    void documentFlagsTextJustOverOneMib() {
        String exact = "x".repeat((int) Document.LARGE_FILE_BYTES);
        assertFalse(Document.isLarge(exact, Document.LARGE_FILE_BYTES));
        assertFalse(new Document("A.java", exact).largeFile());

        String over = exact + "y";
        assertTrue(Document.isLarge(over, -1L));
        assertTrue(new Document("A.java", over).largeFile());
        assertTrue(new Document("A.java", "class A {}", LineEnding.LF, Document.LARGE_FILE_BYTES + 1).largeFile());
    }

    @Test
    void largeFileOpensWithBannerAndNeverAnalyzes() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        byte[] payload = new byte[(int) Document.LARGE_FILE_BYTES + 1];
        // Valid-enough Java so a mistaken analyze would succeed rather than fail open.
        System.arraycopy("class Big {\n".getBytes(StandardCharsets.UTF_8), 0, payload, 0, 11);
        payload[payload.length - 1] = '}';
        for (int i = 11; i < payload.length - 1; i++) {
            payload[i] = ' ';
        }
        Files.write(root.resolve("Big.java"), payload);

        AtomicInteger analyzes = new AtomicInteger();
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Runnable start = () -> {
            try {
                Platform.setImplicitExit(false);
                SettingsStore settings = new SettingsStore(config);
                SessionStore sessions = new SessionStore(config);
                AnalysisScheduler scheduler = new AnalysisScheduler(40, Platform::runLater, (source, path) -> {
                    analyzes.incrementAndGet();
                    return new Analyzer().analyze(source, path);
                });
                IdeWindow window = new IdeWindow(
                        40,
                        DirectoryPrompt.fixed(root),
                        settings,
                        sessions,
                        SaveClosePrompt.dialog(),
                        scheduler);
                Stage stage = new Stage();
                stage.setScene(window.scene());
                window.setOnListed(() -> window.select(child(window, "Big.java")));
                window.setOnOpened(() -> later(450, () -> {
                    try {
                        assertTrue(window.document().largeFile());
                        assertTrue(window.largeFileBannerVisible());
                        assertEquals(0, analyzes.get());
                        assertNull(window.latestAnalysis());
                        assertFalse(window.panelText().contains(MetricsPanel.STALE));
                        assertFalse(window.panelText().contains("Maintainability"));

                        window.editor().insertText(0, " ");
                        later(450, () -> {
                            try {
                                assertEquals(0, analyzes.get());
                                assertNull(window.latestAnalysis());
                                stage.close();
                                window.close();
                                done.countDown();
                            } catch (Throwable thrown) {
                                failure.set(thrown);
                                done.countDown();
                            }
                        });
                    } catch (Throwable thrown) {
                        failure.set(thrown);
                        done.countDown();
                    }
                }));
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
        assertTrue(done.await(30, TimeUnit.SECONDS));
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

    private static void later(long millis, Runnable action) {
        new Thread(() -> {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            }
            Platform.runLater(action);
        }, "large-file-test-wait").start();
    }
}
