package dev.bilal.ccmm.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.bilal.ccmm.app.workbench.Command;
import dev.bilal.ccmm.app.workbench.CommandId;
import dev.bilal.ccmm.app.workbench.CommandRegistry;
import dev.bilal.ccmm.app.workbench.DirectoryPrompt;
import dev.bilal.ccmm.app.workbench.OpenFolderCommand;
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

class CommandRegistryTest {
    @TempDir
    Path root;

    @Test
    void duplicateFileOpenRegistrationFails() {
        CommandRegistry registry = new CommandRegistry();
        Command first = new OpenFolderCommand(DirectoryPrompt.fixed(root), path -> {}, () -> null);
        Command second = new OpenFolderCommand(DirectoryPrompt.fixed(root), path -> {}, () -> null);
        registry.register(first);
        assertThrows(IllegalStateException.class, () -> registry.register(second));
    }

    @Test
    void executingFileOpenListsTheChosenFolder() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        Files.writeString(root.resolve("A.java"), "class A {}");
        Files.writeString(root.resolve("notes.txt"), "hi");

        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Runnable start = () -> {
            try {
                Platform.setImplicitExit(false);
                IdeWindow window = new IdeWindow(40, DirectoryPrompt.fixed(root));
                assertEquals(
                        "Open Folder…",
                        window.workbench().commands().find(CommandId.FILE_OPEN.value()).orElseThrow().title());
                window.setOnListed(() -> {
                    try {
                        assertTrue(window.listedRoot().getChildren().stream()
                                .anyMatch(item -> item.getValue().name().equals("A.java")));
                        Stage stage = (Stage) window.scene().getWindow();
                        stage.close();
                        window.close();
                    } catch (Throwable thrown) {
                        failure.set(thrown);
                    } finally {
                        done.countDown();
                    }
                });
                Stage stage = new Stage();
                stage.setScene(window.scene());
                stage.show();
                window.workbench().execute(CommandId.FILE_OPEN.value());
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
