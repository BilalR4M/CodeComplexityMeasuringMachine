package dev.bilal.ccmm.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.bilal.ccmm.app.session.Session;
import dev.bilal.ccmm.app.session.SessionStore;
import dev.bilal.ccmm.app.settings.SettingsStore;
import dev.bilal.ccmm.app.workbench.DirectoryPrompt;
import java.awt.GraphicsEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SessionRestoreTest {
    @TempDir
    Path configDir;

    @TempDir
    Path projectDir;

    @Test
    void missingFolderBecomesWelcomeWithoutThrowing() {
        Path gone = projectDir.resolve("deleted");
        SessionStore store = new SessionStore(configDir);
        store.save(new Session(gone, List.of("A.java"), true, true, true));
        Session existing = store.existing(store.load());
        assertNull(existing.lastFolder());
        assertTrue(existing.pinned().isEmpty());
    }

    @Test
    void existingDropsMissingPinnedFiles() throws Exception {
        Files.writeString(projectDir.resolve("A.java"), "class A {}");
        SessionStore store = new SessionStore(configDir);
        store.save(new Session(projectDir, List.of("A.java", "Gone.java"), true, false, true));
        Session existing = store.existing(store.load());
        assertEquals(projectDir.toAbsolutePath().normalize(), existing.lastFolder());
        assertEquals(List.of("A.java"), existing.pinned());
        assertFalse(existing.bottomVisible());
    }

    @Test
    void restoreOpensFolderAndPinnedFiles() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        Files.writeString(projectDir.resolve("A.java"), "class A {}");
        Files.writeString(projectDir.resolve("B.java"), "class B {}");
        SessionStore sessions = new SessionStore(configDir);
        sessions.save(new Session(projectDir, List.of("A.java", "B.java"), true, true, true));
        SettingsStore settings = new SettingsStore(configDir);

        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Runnable start = () -> {
            try {
                Platform.setImplicitExit(false);
                IdeWindow window = new IdeWindow(40, DirectoryPrompt.fixed(projectDir), settings, sessions);
                Stage stage = new Stage();
                stage.setScene(window.scene());
                window.setOnRestored(() -> {
                    try {
                        assertFalse(window.isWelcome());
                        assertEquals(projectDir.toAbsolutePath().normalize(), window.projectRoot().toAbsolutePath().normalize());
                        assertEquals(List.of("A.java", "B.java"), window.pinnedPaths());
                        assertEquals("class B {}", window.editor().getText());
                        assertEquals("B.java", window.document().relativePath());
                        stage.close();
                        window.close();
                    } catch (Throwable thrown) {
                        failure.set(thrown);
                    } finally {
                        done.countDown();
                    }
                });
                stage.show();
                window.restoreSession();
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
    void restoreMissingFolderShowsWelcome() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        Path gone = projectDir.resolve("no-such-folder");
        SessionStore sessions = new SessionStore(configDir);
        sessions.save(new Session(gone, List.of("A.java"), true, true, true));
        SettingsStore settings = new SettingsStore(configDir);

        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Runnable start = () -> {
            try {
                Platform.setImplicitExit(false);
                IdeWindow window = new IdeWindow(40, DirectoryPrompt.fixed(projectDir), settings, sessions);
                Stage stage = new Stage();
                stage.setScene(window.scene());
                window.setOnRestored(() -> {
                    try {
                        assertTrue(window.isWelcome());
                        assertNull(window.projectRoot());
                        assertTrue(window.pinnedPaths().isEmpty());
                        assertEquals("", window.editor().getText());
                        stage.close();
                        window.close();
                    } catch (Throwable thrown) {
                        failure.set(thrown);
                    } finally {
                        done.countDown();
                    }
                });
                stage.show();
                window.restoreSession();
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
