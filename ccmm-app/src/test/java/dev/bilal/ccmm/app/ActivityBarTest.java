package dev.bilal.ccmm.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.bilal.ccmm.app.workbench.ActivityView;
import dev.bilal.ccmm.app.workbench.SideViewCopy;
import java.awt.GraphicsEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ActivityBarTest {
    @TempDir
    Path root;

    @Test
    void explorerAccessibleNameIncludesExplorer() {
        assertTrue(ActivityView.EXPLORER.accessibleName().contains("Explorer"));
    }

    @Test
    void searchEmptyCopyUsesFolderOrNoFolder() {
        assertEquals("No folder opened", SideViewCopy.empty(ActivityView.SEARCH, null));
        assertEquals("Type to search in demo", SideViewCopy.empty(ActivityView.SEARCH, "demo"));
    }

    @Test
    void clickingSearchShowsSearchEmptyState() throws Exception {
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
                window.setOnListed(() -> {
                    try {
                        assertTrue(window.activityBar()
                                .button(ActivityView.EXPLORER)
                                .getAccessibleText()
                                .contains("Explorer"));
                        window.activityBar().select(ActivityView.SEARCH);
                        Label empty = (Label) window.sideViews().host().lookup("#side-empty-search");
                        assertTrue(empty.getText().startsWith("Type to search in "));
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
