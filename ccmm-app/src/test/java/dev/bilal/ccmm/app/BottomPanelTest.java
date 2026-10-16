package dev.bilal.ccmm.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.bilal.ccmm.app.workbench.BottomPanel;
import dev.bilal.ccmm.app.workbench.RegionId;
import java.awt.GraphicsEnvironment;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class BottomPanelTest {
    @Test
    void problemsEmptyCopyIsNoProblems() {
        assertEquals("No problems", BottomPanel.PROBLEMS_EMPTY);
    }

    @Test
    void problemsTabShowsNoProblemsAndCtrlJCollapses() throws Exception {
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
                window.bottomPanel().selectProblems();
                assertEquals(BottomPanel.PROBLEMS_EMPTY, window.bottomPanel().problemsText());
                assertTrue(window.workbench().isShowing(RegionId.BOTTOM));

                Runnable toggle = window.scene()
                        .getAccelerators()
                        .get(new KeyCodeCombination(KeyCode.J, KeyCombination.CONTROL_DOWN));
                toggle.run();
                assertFalse(window.workbench().isShowing(RegionId.BOTTOM));
                toggle.run();
                assertTrue(window.workbench().isShowing(RegionId.BOTTOM));

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
