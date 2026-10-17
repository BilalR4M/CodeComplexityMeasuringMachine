package dev.bilal.ccmm.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.bilal.ccmm.app.editor.NavigationHistory;
import dev.bilal.ccmm.app.editor.NavigationHistory.Location;
import dev.bilal.ccmm.app.workbench.CommandId;
import dev.bilal.ccmm.app.workbench.DirectoryPrompt;
import java.awt.GraphicsEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import javafx.stage.Stage;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NavigationHistoryTest {
    @TempDir
    Path root;

    @Test
    void jumpOverTwentyLinesPushesAndBackReturns() {
        NavigationHistory history = new NavigationHistory();
        history.record("A.java", 0, 1);
        history.record("A.java", 100, 5); // small move — no push
        assertEquals(0, history.backSize());
        history.record("A.java", 800, 40); // > 20 lines from last recorded (line 1)
        assertEquals(1, history.backSize());

        Optional<Location> previous = history.back(new Location("A.java", 800, 40));
        assertTrue(previous.isPresent());
        assertEquals(1, previous.get().line());
        assertEquals(0, history.backSize());
        assertTrue(history.canGoForward());
    }

    @Test
    void backDoesNotPushAnotherEntry() {
        NavigationHistory history = new NavigationHistory();
        history.record("A.java", 0, 1);
        history.record("A.java", 900, 50);
        int sizeBefore = history.backSize();
        Location current = new Location("A.java", 900, 50);
        history.back(current);
        // Applying back must not grow the back stack via record
        history.record("A.java", 0, 1);
        assertEquals(sizeBefore - 1, history.backSize());
    }

    @Test
    void fileChangePushesEvenWithinTwentyLines() {
        NavigationHistory history = new NavigationHistory();
        history.record("A.java", 0, 1);
        history.record("B.java", 0, 2);
        assertEquals(1, history.backSize());
    }

    @Test
    void altLeftReturnsCaretAfterJumpPastThreshold() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        StringBuilder body = new StringBuilder("class A {\n");
        for (int i = 0; i < 50; i++) {
            body.append("  int v").append(i).append(" = ").append(i).append(";\n");
        }
        body.append("}\n");
        Files.writeString(root.resolve("A.java"), body.toString());

        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Runnable start = () -> {
            try {
                Platform.setImplicitExit(false);
                IdeWindow window = new IdeWindow(40, DirectoryPrompt.fixed(root));
                Stage stage = new Stage();
                stage.setScene(window.scene());
                window.setOnListed(() -> window.select(child(window, "A.java")));
                window.setOnOpened(() -> later(80, () -> {
                    try {
                        window.editor().moveTo(0);
                        assertEquals(1, window.editor().getCurrentParagraph() + 1);
                        // Jump to line 40 (0-based paragraph 39)
                        window.editor().moveTo(39, 0);
                        assertTrue(window.navigationHistory().canGoBack());
                        int beforeBack = window.navigationHistory().backSize();
                        window.workbench().execute(CommandId.NAV_BACK.value());
                        assertEquals(1, window.editor().getCurrentParagraph() + 1);
                        assertEquals(beforeBack - 1, window.navigationHistory().backSize());
                        // Back must not have added via record
                        assertFalse(window.navigationHistory().backSize() > beforeBack);
                        stage.close();
                        window.close();
                        done.countDown();
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
        }, "nav-history-wait").start();
    }
}
