package dev.bilal.ccmm.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.bilal.ccmm.app.editor.GoToLine;
import java.awt.GraphicsEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.OptionalInt;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import javafx.stage.Stage;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GoToLineTest {
    @TempDir
    Path root;

    @Test
    void parseKeepsDigitsOnly() {
        assertEquals(OptionalInt.of(4), GoToLine.parse("4"));
        assertEquals(OptionalInt.of(12), GoToLine.parse("1a2"));
        assertTrue(GoToLine.parse("").isEmpty());
        assertTrue(GoToLine.parse("abc").isEmpty());
        assertTrue(GoToLine.parse("0").isEmpty());
    }

    @Test
    void clampReportsPastEnd() {
        GoToLine.Target ok = GoToLine.clamp(4, 10);
        assertEquals(4, ok.line());
        assertFalse(ok.pastEnd());

        GoToLine.Target past = GoToLine.clamp(99, 10);
        assertEquals(10, past.line());
        assertTrue(past.pastEnd());
        assertEquals(99, past.requested());
        assertEquals("Line 99 is past the end", GoToLine.pastEndMessage(99));
    }

    @Test
    void enteringFourMovesCaretToLineFour() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        Files.writeString(root.resolve("A.java"), "1\n2\n3\n4\n5\n6\n7\n8\n9\n10");

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
                        window.workbench().execute(dev.bilal.ccmm.app.workbench.CommandId.GO_TO_LINE.value());
                        assertTrue(window.goToLineBar().isShowing());
                        window.goToLineBar().field().setText("4");
                        window.goToLineBar().field().fireEvent(new javafx.event.ActionEvent());
                        assertEquals(3, window.editor().getCurrentParagraph());
                        assertEquals(0, window.editor().getCaretColumn());
                        assertFalse(window.goToLineBar().isShowing());
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
    void pastEndClampsAndSetsStatus() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        Files.writeString(root.resolve("A.java"), "1\n2\n3\n4\n5\n6\n7\n8\n9\n10");

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
                        window.goToLineBar().show();
                        window.goToLineBar().field().setText("99");
                        window.goToLineBar().field().fireEvent(new javafx.event.ActionEvent());
                        assertEquals(9, window.editor().getCurrentParagraph());
                        assertEquals("Line 99 is past the end", window.workbench().statusBar().leftText());
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
