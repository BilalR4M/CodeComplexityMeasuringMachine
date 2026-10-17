package dev.bilal.ccmm.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.bilal.ccmm.app.document.Document;
import dev.bilal.ccmm.app.editor.SaveClosePrompt;
import dev.bilal.ccmm.app.editor.TabStrip;
import dev.bilal.ccmm.app.settings.SettingsStore;
import dev.bilal.ccmm.app.workbench.DirectoryPrompt;
import java.awt.GraphicsEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import javafx.stage.Stage;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TabStripTest {
    @TempDir
    Path root;

    @TempDir
    Path configDir;

    @Test
    void previewTabIsReplacedByNextSingleClick() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        Files.writeString(root.resolve("A.java"), "class A {}");
        Files.writeString(root.resolve("B.java"), "class B {}");

        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Runnable start = () -> {
            try {
                Platform.setImplicitExit(false);
                IdeWindow window = new IdeWindow(
                        40, DirectoryPrompt.fixed(root), new SettingsStore(configDir), SaveClosePrompt.always(SaveClosePrompt.Choice.CANCEL));
                Stage stage = new Stage();
                stage.setScene(window.scene());
                window.setOnListed(() -> window.select(child(window, "A.java")));
                window.setOnOpened(() -> {
                    try {
                        if (window.editor().getText().equals("class A {}")) {
                            window.setOnOpened(() -> {
                                try {
                                    assertEquals(1, window.editorTabs().tabs().size());
                                    assertEquals("B.java", window.editorTabs().active().orElseThrow().relativePath());
                                    assertTrue(window.editorTabs().active().orElseThrow().preview());
                                    assertEquals("class B {}", window.editor().getText());
                                    assertTrue(window.tabStrip().openTabsButton().isVisible());
                                    assertEquals(TabStrip.OPEN_TABS, window.tabStrip().openTabsButton().getText());
                                    stage.close();
                                    window.close();
                                    done.countDown();
                                } catch (Throwable thrown) {
                                    failure.set(thrown);
                                    done.countDown();
                                }
                            });
                            window.select(child(window, "B.java"));
                        }
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
    void dirtyCloseCancelKeepsTheTab() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        Files.writeString(root.resolve("A.java"), "class A {}");

        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Runnable start = () -> {
            try {
                Platform.setImplicitExit(false);
                IdeWindow window = new IdeWindow(
                        40,
                        DirectoryPrompt.fixed(root),
                        new SettingsStore(configDir),
                        SaveClosePrompt.always(SaveClosePrompt.Choice.CANCEL));
                Stage stage = new Stage();
                stage.setScene(window.scene());
                window.setOnListed(() -> window.select(child(window, "A.java")));
                window.setOnOpened(() -> {
                    try {
                        window.editor().insertText(0, "//");
                        assertTrue(window.document().dirty());
                        window.workbench().execute("tab.close");
                        assertEquals(1, window.editorTabs().tabs().size());
                        assertTrue(window.document().dirty());
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
    void openTabsListsManyTabs() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        for (int i = 0; i < 8; i++) {
            Files.writeString(root.resolve("F" + i + ".java"), "class F" + i + " {}");
        }
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Runnable start = () -> {
            try {
                Platform.setImplicitExit(false);
                IdeWindow window = new IdeWindow(
                        40, DirectoryPrompt.fixed(root), new SettingsStore(configDir), SaveClosePrompt.dialog());
                Stage stage = new Stage();
                stage.setScene(window.scene());
                stage.setWidth(400);
                window.setOnListed(() -> {
                    try {
                        for (int i = 0; i < 8; i++) {
                            Document doc = new Document("F" + i + ".java", "class F" + i + " {}");
                            window.editorTabs().openPinned(doc);
                        }
                        window.tabStrip().refresh();
                        assertEquals(8, window.editorTabs().tabs().size());
                        assertTrue(window.tabStrip().openTabsButton().isVisible());
                        assertEquals(8, window.tabStrip().openTabsButton().getItems().size());
                        assertEquals(TabStrip.OPEN_TABS, window.tabStrip().openTabsButton().getAccessibleText());
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

    private static TreeItem<FileNode> child(IdeWindow window, String name) {
        return window.listedRoot().getChildren().stream()
                .filter(item -> item.getValue().name().equals(name))
                .findFirst()
                .orElseThrow();
    }
}
