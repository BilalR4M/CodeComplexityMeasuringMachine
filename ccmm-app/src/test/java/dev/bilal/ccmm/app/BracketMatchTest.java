package dev.bilal.ccmm.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.bilal.ccmm.app.editor.BracketMatcher;
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

class BracketMatchTest {
    @TempDir
    Path root;

    @Test
    void openingParenMatchesClosingInIfOk() {
        String text = "if (ok) { }";
        int open = text.indexOf('(');
        Optional<BracketMatcher.Match> match = BracketMatcher.find(text, open + 1);
        assertTrue(match.isPresent());
        assertEquals(open, match.get().open());
        assertEquals(text.indexOf(')'), match.get().close());
    }

    @Test
    void bracePairMatches() {
        String text = "if (ok) { }";
        int open = text.indexOf('{');
        Optional<BracketMatcher.Match> match = BracketMatcher.find(text, open + 1);
        assertTrue(match.isPresent());
        assertEquals(open, match.get().open());
        assertEquals(text.indexOf('}'), match.get().close());
    }

    @Test
    void bracketInsideStringDoesNotMatchCode() {
        String text = "x = \"(\"; y = (1);";
        int stringParen = text.indexOf('(');
        assertEquals('"', text.charAt(stringParen - 1));
        assertTrue(BracketMatcher.find(text, stringParen + 1).isEmpty());

        int codeOpen = text.lastIndexOf('(');
        Optional<BracketMatcher.Match> match = BracketMatcher.find(text, codeOpen + 1);
        assertTrue(match.isPresent());
        assertEquals(codeOpen, match.get().open());
        assertEquals(text.lastIndexOf(')'), match.get().close());
    }

    @Test
    void commentBracketIsIgnored() {
        String text = "// (\nx = (1);";
        int commentParen = text.indexOf('(');
        assertTrue(BracketMatcher.find(text, commentParen + 1).isEmpty());
        int codeOpen = text.lastIndexOf('(');
        assertTrue(BracketMatcher.find(text, codeOpen + 1).isPresent());
    }

    @Test
    void caretOnOpeningParenHighlightsPairInEditor() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        Files.writeString(root.resolve("A.java"), "if (ok) { }");

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
                        String text = window.editor().getText();
                        int open = text.indexOf('(');
                        window.editor().moveTo(open + 1);
                        int[] highlight = window.bracketHighlight();
                        assertEquals(open, highlight[0]);
                        assertEquals(text.indexOf(')'), highlight[1]);
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
