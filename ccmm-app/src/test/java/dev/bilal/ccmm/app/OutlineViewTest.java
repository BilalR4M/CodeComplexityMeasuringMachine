package dev.bilal.ccmm.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.bilal.ccmm.app.workbench.ActivityView;
import dev.bilal.ccmm.app.workbench.DirectoryPrompt;
import dev.bilal.ccmm.core.AnalysisResult;
import dev.bilal.ccmm.core.MethodMetrics;
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

class OutlineViewTest {
    @TempDir
    Path root;

    @Test
    void emptyStatesUseSpecCopy() {
        OutlineView view = new OutlineView();
        assertEquals(OutlineView.NO_DOCUMENT, view.message().getText());
        view.showNoOutline();
        assertEquals(OutlineView.NO_OUTLINE, view.message().getText());
        view.showAnalyzing();
        assertEquals(OutlineView.ANALYZING, view.message().getText());
    }

    @Test
    void twoMethodsAppearAndClickMovesCaret() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        Files.writeString(
                root.resolve("A.java"),
                """
                class A {
                    void first() {}
                    void second() { if (true) {} }
                }
                """);

        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Runnable start = () -> {
            try {
                Platform.setImplicitExit(false);
                IdeWindow window = new IdeWindow(40, DirectoryPrompt.fixed(root));
                Stage stage = new Stage();
                stage.setScene(window.scene());
                window.setOnListed(() -> window.select(child(window, "A.java")));
                window.setOnAnalyzed(() -> {
                    try {
                        AnalysisResult.Ok ok = (AnalysisResult.Ok) window.latestAnalysis();
                        assertTrue(ok.file().methods().size() >= 2);
                        window.activityBar().select(ActivityView.OUTLINE);
                        List<MethodMetrics> rows = List.copyOf(window.outlineView().list().getItems());
                        assertTrue(rows.stream().anyMatch(m -> "first".equals(m.name())));
                        assertTrue(rows.stream().anyMatch(m -> "second".equals(m.name())));
                        assertFalse(window.outlineView().message().isVisible());

                        MethodMetrics second = rows.stream()
                                .filter(m -> "second".equals(m.name()))
                                .findFirst()
                                .orElseThrow();
                        window.outlineView().list().getSelectionModel().select(second);
                        window.outlineView().activateSelected();
                        assertEquals(second.startLine() - 1, window.editor().getCurrentParagraph());
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
    void noDocumentShowsOpenAFileCopy() throws Exception {
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
                window.activityBar().select(ActivityView.OUTLINE);
                assertEquals(OutlineView.NO_DOCUMENT, window.outlineView().message().getText());
                assertTrue(window.outlineView().message().isVisible());
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
}
