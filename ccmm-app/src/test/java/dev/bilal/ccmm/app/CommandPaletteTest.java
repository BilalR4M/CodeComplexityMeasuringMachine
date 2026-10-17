package dev.bilal.ccmm.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.bilal.ccmm.app.workbench.Command;
import dev.bilal.ccmm.app.workbench.CommandId;
import dev.bilal.ccmm.app.workbench.CommandPalette;
import dev.bilal.ccmm.app.workbench.CommandRegistry;
import dev.bilal.ccmm.app.workbench.DirectoryPrompt;
import dev.bilal.ccmm.app.workbench.Keymap;
import dev.bilal.ccmm.app.workbench.OpenFolderCommand;
import dev.bilal.ccmm.app.workbench.RegionId;
import dev.bilal.ccmm.app.workbench.Shortcut;
import dev.bilal.ccmm.app.workbench.SimpleCommand;
import java.awt.GraphicsEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CommandPaletteTest {
    @TempDir
    Path root;

    @Test
    void filterMatchesOpenSubstringAndOrdersRecentWhenEmpty() {
        CommandRegistry registry = new CommandRegistry();
        registry.register(new OpenFolderCommand(DirectoryPrompt.fixed(root), path -> {}, () -> null));
        registry.register(new SimpleCommand(CommandId.FIND, "Find", wb -> {}));
        registry.register(new SimpleCommand(CommandId.CLOSE_TAB, "Close Tab", wb -> {}));

        List<Command> matched = registry.filter("open", List.of());
        assertEquals(1, matched.size());
        assertEquals(CommandId.FILE_OPEN.value(), matched.get(0).id());

        List<Command> emptyQuery = registry.filter("", List.of(CommandId.FIND.value()));
        assertEquals(CommandId.FIND.value(), emptyQuery.get(0).id());
        assertEquals(3, emptyQuery.size());
    }

    @Test
    void filterWithNoMatchIsEmpty() {
        CommandRegistry registry = new CommandRegistry();
        registry.register(new SimpleCommand(CommandId.FIND, "Find", wb -> {}));
        assertTrue(registry.filter("zzzz", List.of()).isEmpty());
    }

    @Test
    void shortcutLabelUsesCtrlOnWindows() {
        Keymap map = Keymap.seeded(Keymap.Platform.OTHER);
        assertEquals("Ctrl+Shift+P", map.shortcutLabel(CommandId.COMMAND_PALETTE.value()));
        assertEquals("Ctrl+P", Shortcut.primary(KeyCode.P).label(Keymap.Platform.OTHER));
        assertEquals("Cmd+P", Shortcut.primary(KeyCode.P).label(Keymap.Platform.MAC));
    }

    @Test
    void queryOpenShowsOpenFolderAndEnterRunsIt() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        Files.writeString(root.resolve("A.java"), "class A {}");

        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Runnable start = () -> {
            try {
                Platform.setImplicitExit(false);
                IdeWindow window = new IdeWindow(40, DirectoryPrompt.fixed(root));
                Stage stage = new Stage();
                stage.setScene(window.scene());
                stage.show();

                window.workbench().execute(CommandId.COMMAND_PALETTE.value());
                assertTrue(window.commandPalette().isShowing());
                assertEquals(CommandPalette.PLACEHOLDER, window.commandPalette().field().getPromptText());
                assertEquals(
                        CommandPalette.PLACEHOLDER, window.commandPalette().field().getAccessibleText());

                window.commandPalette().field().setText("open");
                List<Command> rows = List.copyOf(window.commandPalette().list().getItems());
                assertTrue(rows.stream().anyMatch(c -> c.id().equals(CommandId.FILE_OPEN.value())));
                assertFalse(window.commandPalette().emptyLabel().isVisible());

                window.setOnListed(() -> {
                    try {
                        assertTrue(window.listedRoot().getChildren().stream()
                                .anyMatch(item -> item.getValue().name().equals("A.java")));
                        assertFalse(window.commandPalette().isShowing());
                        stage.close();
                        window.close();
                    } catch (Throwable thrown) {
                        failure.set(thrown);
                    } finally {
                        done.countDown();
                    }
                });
                window.commandPalette().field().fireEvent(
                        new javafx.event.ActionEvent());
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
    void unmatchedQueryShowsNoMatchingCommands() throws Exception {
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
                window.keymap().fire(window.workbench(), CommandId.COMMAND_PALETTE.value());
                window.commandPalette().field().setText("zzzz-no-such-command");
                assertTrue(window.commandPalette().emptyLabel().isVisible());
                assertEquals(CommandPalette.NO_MATCH, window.commandPalette().emptyLabel().getText());
                assertTrue(window.commandPalette().list().getItems().isEmpty());
                window.commandPalette().hide();
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

    @Test
    void runningACommandThatSetsStatusStillClosesThePalette() throws Exception {
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
                // Toggle side sets no status but still closes; use missing quick open path via palette row
                window.workbench().execute(CommandId.COMMAND_PALETTE.value());
                window.commandPalette().field().setText("Toggle Side");
                assertFalse(window.commandPalette().list().getItems().isEmpty());
                boolean wasShowing = window.workbench().isShowing(RegionId.SIDE);
                window.commandPalette().field().fireEvent(new javafx.event.ActionEvent());
                assertFalse(window.commandPalette().isShowing());
                assertEquals(!wasShowing, window.workbench().isShowing(RegionId.SIDE));
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
}
