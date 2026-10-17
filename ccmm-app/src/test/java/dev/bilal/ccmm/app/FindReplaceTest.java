package dev.bilal.ccmm.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.bilal.ccmm.app.editor.FindReplaceBar;
import java.awt.GraphicsEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import javafx.stage.Stage;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FindReplaceTest {
    @TempDir
    Path root;

    @Test
    void nextTwiceSelectsTheSecondParseMatch() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        Files.writeString(root.resolve("A.java"), "parse once parse twice");

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
                        window.editor().moveTo(0);
                        window.findReplaceBar().show(false);
                        window.findReplaceBar().findField().setText("parse");
                        ((Button) window.findReplaceBar().root().lookup("#find-next")).fire();
                        later(250, () -> {
                            try {
                                assertEquals("parse", window.editor().getSelectedText());
                                assertEquals(0, window.editor().getSelection().getStart());
                                ((Button) window.findReplaceBar().root().lookup("#find-next")).fire();
                                assertEquals("parse", window.editor().getSelectedText());
                                assertEquals(11, window.editor().getSelection().getStart());
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
    void replaceAllIsOneUndoStepAndMarksDirty() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        Files.writeString(root.resolve("A.java"), "a a");

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
                        window.findReplaceBar().show(true);
                        window.findReplaceBar().findField().setText("a");
                        window.findReplaceBar().replaceField().setText("b");
                        ((Button) window.findReplaceBar().root().lookup("#find-replace-all")).fire();
                        later(250, () -> {
                            try {
                                assertEquals("b b", window.editor().getText());
                                assertTrue(window.document().dirty());
                                assertEquals("Replaced 2", window.workbench().statusBar().leftText());
                                assertTrue(window.editor().getUndoManager().undo());
                                assertEquals("a a", window.editor().getText());
                                assertFalse(window.editor().getUndoManager().isUndoAvailable());
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
    void findAndReplaceFieldsExposeAccessibleNames() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Runnable start = () -> {
            try {
                Platform.setImplicitExit(false);
                IdeWindow window = new IdeWindow();
                Stage stage = new Stage();
                stage.setScene(window.scene());
                stage.show();
                FindReplaceBar bar = window.findReplaceBar();
                assertEquals(FindReplaceBar.FIND_NAME, bar.findField().getAccessibleText());
                assertEquals(FindReplaceBar.REPLACE_NAME, bar.replaceField().getAccessibleText());
                window.workbench().execute(dev.bilal.ccmm.app.workbench.CommandId.FIND.value());
                assertTrue(bar.isShowing());
                assertFalse(bar.replaceMode());
                window.workbench().execute(dev.bilal.ccmm.app.workbench.CommandId.REPLACE.value());
                assertTrue(bar.replaceMode());
                stage.close();
                window.close();
                done.countDown();
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

    private static void later(long millis, Runnable action) {
        new Thread(() -> {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            }
            Platform.runLater(action);
        }, "find-replace-test-wait").start();
    }
}
