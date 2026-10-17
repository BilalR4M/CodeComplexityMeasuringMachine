package dev.bilal.ccmm.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.bilal.ccmm.app.document.LineEnding;
import dev.bilal.ccmm.app.document.Utf8Text;
import dev.bilal.ccmm.app.editor.SaveClosePrompt;
import dev.bilal.ccmm.app.settings.SettingsStore;
import java.awt.GraphicsEnvironment;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import javafx.stage.Stage;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LineEndingTest {
    @TempDir
    Path root;

    @TempDir
    Path configDir;

    @Test
    void detectPrefersCrlfWhenPresent() {
        assertEquals(LineEnding.CRLF, LineEnding.detect("a\r\nb\n"));
        assertEquals(LineEnding.LF, LineEnding.detect("a\nb"));
        assertEquals("a\r\nb\r\n", LineEnding.CRLF.encode("a\nb\n"));
        assertEquals("a\nb\n", LineEnding.LF.encode("a\r\nb\r\n"));
    }

    @Test
    void crlfFileShowsCrlfAndUtf8InStatus() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        Files.writeString(root.resolve("A.java"), "class A {\r\n}\r\n", StandardCharsets.UTF_8);

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
                        assertEquals(LineEnding.CRLF.label(), window.workbench().statusBar().eolText());
                        assertEquals(Utf8Text.ENCODING_LABEL, window.workbench().statusBar().encodingText());
                        assertEquals("class A {\n}\n", window.editor().getText());
                        stage.close();
                        window.close();
                        done.countDown();
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
        assertTrue(done.await(20, TimeUnit.SECONDS));
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
    }

    @Test
    void switchingToLfAndSavingWritesOnlyLf() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        Path file = root.resolve("A.java");
        Files.writeString(file, "a\r\nb\r\n", StandardCharsets.UTF_8);

        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Runnable start = () -> {
            try {
                Platform.setImplicitExit(false);
                IdeWindow window = new IdeWindow(
                        40,
                        dev.bilal.ccmm.app.workbench.DirectoryPrompt.fixed(root),
                        new SettingsStore(configDir),
                        SaveClosePrompt.always(SaveClosePrompt.Choice.SAVE));
                Stage stage = new Stage();
                stage.setScene(window.scene());
                window.setOnListed(() -> window.select(child(window, "A.java")));
                window.setOnOpened(() -> {
                    try {
                        MenuItem lf = window.workbench().statusBar().eolButton().getItems().stream()
                                .filter(item -> LineEnding.LF.label().equals(item.getText()))
                                .findFirst()
                                .orElseThrow();
                        lf.fire();
                        assertTrue(window.document().dirty());
                        assertEquals(LineEnding.LF, window.document().lineEnding());
                        window.saveActiveDocument(() -> {
                            try {
                                byte[] bytes = Files.readAllBytes(file);
                                String saved = new String(bytes, StandardCharsets.UTF_8);
                                assertFalse(saved.contains("\r"));
                                assertTrue(saved.contains("\n"));
                                assertEquals("a\nb\n", saved);
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
        assertTrue(done.await(20, TimeUnit.SECONDS));
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
    }

    @Test
    void badUtf8KeepsPreviousDocument() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        Files.writeString(root.resolve("Good.java"), "class Good {}", StandardCharsets.UTF_8);
        Files.write(root.resolve("Bad.java"), new byte[] {'c', 'l', 'a', 's', 's', ' ', (byte) 0xFF, ';'});

        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Runnable start = () -> {
            try {
                Platform.setImplicitExit(false);
                IdeWindow window = new IdeWindow();
                Stage stage = new Stage();
                stage.setScene(window.scene());
                window.setOnListed(() -> window.select(child(window, "Good.java")));
                window.setOnOpened(() -> {
                    try {
                        assertEquals("class Good {}", window.editor().getText());
                        window.select(child(window, "Bad.java"));
                        later(300, () -> {
                            try {
                                assertEquals(
                                        Utf8Text.couldNotReadMessage("Bad.java"),
                                        window.workbench().statusBar().leftText());
                                assertEquals("class Good {}", window.editor().getText());
                                assertEquals("Good.java", window.document().relativePath());
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
        assertTrue(done.await(20, TimeUnit.SECONDS));
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
        }, "line-ending-test-wait").start();
    }
}
