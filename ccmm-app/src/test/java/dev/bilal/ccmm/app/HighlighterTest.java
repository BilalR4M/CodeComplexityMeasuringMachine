package dev.bilal.ccmm.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.bilal.ccmm.app.editor.HighlightScheduler;
import dev.bilal.ccmm.app.editor.HighlightSpan;
import dev.bilal.ccmm.app.editor.RegexJavaHighlighter;
import java.awt.GraphicsEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
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

class HighlighterTest {
    @TempDir
    Path root;

    @Test
    void keywordPublicIsTokenized() {
        RegexJavaHighlighter highlighter = new RegexJavaHighlighter();
        String text = "public class A {}";
        List<HighlightSpan> spans = highlighter.highlight(text, "A.java");
        assertTrue(spans.stream()
                .anyMatch(span -> "keyword".equals(span.token())
                        && "public".equals(text.substring(span.start(), span.end()))));
    }

    @Test
    void nonJavaPathYieldsNoSpans() {
        assertTrue(new RegexJavaHighlighter().highlight("public int x;", "notes.md").isEmpty());
    }

    @Test
    void newerEditDropsStaleSpans() throws Exception {
        AtomicInteger staleApplied = new AtomicInteger();
        CountDownLatch freshApplied = new CountDownLatch(1);
        HighlightScheduler scheduler = new HighlightScheduler(Runnable::run);
        String bulky = "/*" + "x".repeat(200_000) + "*/\npublic class A {}";

        scheduler.schedule(bulky, "A.java", spans -> staleApplied.incrementAndGet());
        scheduler.schedule("public class B {}", "B.java", spans -> freshApplied.countDown());

        assertTrue(freshApplied.await(5, TimeUnit.SECONDS));
        Thread.sleep(50);
        assertEquals(0, staleApplied.get());
        scheduler.close();
    }

    @Test
    void javaMethodKeywordIsStyledOffFxThread() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        Files.writeString(root.resolve("A.java"), "public class A { void m() {} }");

        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Runnable start = () -> {
            try {
                Platform.setImplicitExit(false);
                IdeWindow window = new IdeWindow();
                Stage stage = new Stage();
                stage.setScene(window.scene());
                window.setOnListed(() -> window.select(child(window, "A.java")));
                window.setOnOpened(() -> later(250, () -> {
                    try {
                        List<HighlightSpan> spans = window.highlightSpans();
                        assertTrue(spans.stream()
                                .anyMatch(span -> "keyword".equals(span.token())
                                        && "public"
                                                .equals(window.editor()
                                                        .getText()
                                                        .substring(span.start(), span.end()))));
                        Collection<?> style = window.editor().getStyleAtPosition(0);
                        assertTrue(style.stream().anyMatch(s -> "token-keyword".equals(String.valueOf(s))));
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
        }, "highlighter-test-wait").start();
    }
}
