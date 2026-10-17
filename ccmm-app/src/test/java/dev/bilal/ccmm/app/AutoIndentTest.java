package dev.bilal.ccmm.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.bilal.ccmm.app.editor.AutoIndent;
import java.awt.GraphicsEnvironment;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class AutoIndentTest {
    @Test
    void plainLineKeepsLeadingSpaces() {
        assertEquals("    ", AutoIndent.indentAfter("    return;"));
        assertEquals("\n    ", AutoIndent.insertion("    return;"));
    }

    @Test
    void braceLineAddsOneLevel() {
        assertEquals("        ", AutoIndent.indentAfter("    if (ok) {"));
        assertEquals("\n        ", AutoIndent.insertion("    if (ok) {"));
    }

    @Test
    void enterAfterReturnIndentsFourSpaces() throws Exception {
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
                window.editor().replaceText("    return;");
                window.editor().moveTo(window.editor().getLength());
                fireEnter(window);
                assertEquals("    return;\n    ", window.editor().getText());
                assertTrue(window.editor().getCaretPosition() >= window.editor().getText().length() - 4);
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
    void enterAfterBraceIndentsEightSpaces() throws Exception {
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
                window.editor().replaceText("    if (ok) {");
                window.editor().moveTo(window.editor().getLength());
                fireEnter(window);
                assertEquals("    if (ok) {\n        ", window.editor().getText());
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

    private static void fireEnter(IdeWindow window) {
        KeyEvent enter = new KeyEvent(
                KeyEvent.KEY_PRESSED, "\r", "\r", KeyCode.ENTER, false, false, false, false);
        window.editor().fireEvent(enter);
    }
}
