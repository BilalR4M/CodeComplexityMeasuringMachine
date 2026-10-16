package dev.bilal.ccmm.app;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.bilal.ccmm.app.workbench.RegionId;
import java.awt.GraphicsEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorkbenchLayoutTest {
    @TempDir
    Path root;

    @Test
    void editorColumnIsAtLeastSixtyFivePercentAtTwelveEighty() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        Files.writeString(root.resolve("A.java"), "class A {}");

        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Runnable start = () -> {
            try {
                Platform.setImplicitExit(false);
                IdeWindow window = new IdeWindow();
                Stage stage = new Stage();
                stage.setScene(window.scene());
                stage.setWidth(1280);
                stage.setHeight(800);
                window.setOnListed(() -> {
                    try {
                        Platform.runLater(() -> {
                            try {
                                window.workbench().show(RegionId.SIDE, true);
                                window.workbench().show(RegionId.INSPECTOR, true);
                                window.workbench().show(RegionId.BOTTOM, true);
                                stage.setWidth(1280);
                                stage.setHeight(800);
                                Platform.runLater(() -> {
                                    try {
                                        // Client area is what the workbench lays out against.
                                        double width = window.workbench().root().getWidth();
                                        double editor = window.workbench().editorColumnWidth();
                                        assertTrue(width >= 1200, "root width was " + width);
                                        assertTrue(
                                                editor / width >= 0.65,
                                                "editor share was " + (editor / width) + " at width "
                                                        + width);
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
    void inspectorStartsCollapsedBelowElevenHundred() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());

        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Runnable start = () -> {
            try {
                Platform.setImplicitExit(false);
                IdeWindow window = new IdeWindow();
                Stage stage = new Stage();
                stage.setScene(window.scene());
                stage.setWidth(1000);
                stage.setHeight(700);
                stage.show();
                Platform.runLater(() -> {
                    try {
                        assertFalse(window.workbench().isShowing(RegionId.INSPECTOR));
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
    void openFolderStillListsFiles() throws Exception {
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
                        assertTrue(window.listedRoot().getChildren().stream()
                                .anyMatch(item -> item.getValue().name().equals("A.java")));
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
}
