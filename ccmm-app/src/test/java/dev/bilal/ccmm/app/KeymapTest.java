package dev.bilal.ccmm.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.bilal.ccmm.app.workbench.CommandId;
import dev.bilal.ccmm.app.workbench.Keymap;
import dev.bilal.ccmm.app.workbench.RegionId;
import dev.bilal.ccmm.app.workbench.Shortcut;
import dev.bilal.ccmm.app.workbench.Workbench;
import java.awt.GraphicsEnvironment;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class KeymapTest {
    @Test
    void macPrimaryPMapsToQuickOpen() {
        Keymap map = Keymap.seeded(Keymap.Platform.MAC);
        assertEquals(
                CommandId.QUICK_OPEN.value(),
                map.commandId(Shortcut.primary(KeyCode.P)).orElseThrow());
        var combo = map.combination(Shortcut.primary(KeyCode.P));
        assertEquals(KeyCombination.ModifierValue.DOWN, combo.getMeta());
        assertEquals(KeyCombination.ModifierValue.UP, combo.getControl());
    }

    @Test
    void windowsPrimaryPMapsToQuickOpen() {
        Keymap map = Keymap.seeded(Keymap.Platform.OTHER);
        assertEquals(
                CommandId.QUICK_OPEN.value(),
                map.commandId(Shortcut.primary(KeyCode.P)).orElseThrow());
        var combo = map.combination(Shortcut.primary(KeyCode.P));
        assertEquals(KeyCombination.ModifierValue.DOWN, combo.getControl());
        assertEquals(KeyCombination.ModifierValue.UP, combo.getMeta());
    }

    @Test
    void missingCommandSetsStatusNotAvailable() {
        Workbench workbench = new Workbench();
        Keymap map = Keymap.seeded(Keymap.Platform.OTHER);
        map.fire(workbench, CommandId.QUICK_OPEN.value());
        assertEquals(Keymap.NOT_AVAILABLE, workbench.statusBar().leftText());
    }

    @Test
    void ctrlBAndCmdBMapToTheSameToggleSideCommand() {
        assertEquals(
                CommandId.VIEW_TOGGLE_SIDE.value(),
                Keymap.seeded(Keymap.Platform.MAC)
                        .commandId(Shortcut.primary(KeyCode.B))
                        .orElseThrow());
        assertEquals(
                CommandId.VIEW_TOGGLE_SIDE.value(),
                Keymap.seeded(Keymap.Platform.OTHER)
                        .commandId(Shortcut.primary(KeyCode.B))
                        .orElseThrow());
    }

    @Test
    void ctrlBTogglesSideBarThroughTheKeymap() throws Exception {
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
                assertTrue(window.workbench().isShowing(RegionId.SIDE));
                window.keymap().fire(window.workbench(), CommandId.VIEW_TOGGLE_SIDE.value());
                assertFalse(window.workbench().isShowing(RegionId.SIDE));
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
