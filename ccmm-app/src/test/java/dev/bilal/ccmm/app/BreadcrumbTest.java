package dev.bilal.ccmm.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.bilal.ccmm.app.editor.Breadcrumbs;
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

class BreadcrumbTest {
    @TempDir
    Path root;

    @Test
    void segmentsJoinPathAndMethod() {
        assertEquals(
                "src > app > Foo.java > parse",
                Breadcrumbs.join(Breadcrumbs.segments("src/app/Foo.java", "parse")));
        assertEquals("notes.md", Breadcrumbs.join(Breadcrumbs.segments("notes.md", null)));
    }

    @Test
    void caretInParseShowsPathAndMethod() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        Path src = Files.createDirectories(root.resolve("src").resolve("app"));
        Files.writeString(
                src.resolve("Foo.java"),
                """
                package app;
                class Foo {
                    void other() {}
                    void parse() { if (true) {} }
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
                window.setOnListed(() -> window.select(find(window.listedRoot(), "Foo.java")));
                window.setOnAnalyzed(() -> {
                    try {
                        String text = window.editor().getText();
                        int parseAt = text.indexOf("void parse");
                        assertTrue(parseAt >= 0);
                        window.editor().moveTo(parseAt + 5);
                        assertEquals(
                                "src > app > Foo.java > parse",
                                window.breadcrumbBar().text());
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

    private static TreeItem<FileNode> find(TreeItem<FileNode> node, String name) {
        if (node.getValue() != null && name.equals(node.getValue().name())) {
            return node;
        }
        for (TreeItem<FileNode> child : node.getChildren()) {
            TreeItem<FileNode> found = find(child, name);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
