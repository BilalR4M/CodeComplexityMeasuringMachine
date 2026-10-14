package dev.bilal.ccmm.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.bilal.ccmm.collab.protocol.Message;
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

class ProjectRoomTest {
    @TempDir
    Path root;

    @Test
    void presenceAndChatAreShownAndTextOpsAreIgnored() {
        ProjectRoom room = new ProjectRoom();
        room.accept(new Message.Presence("Ada", true));
        room.accept(new Message.Presence("Bob", true));
        room.accept(new Message.Chat("Ada", "hello"));
        room.accept(new Message.Presence("Bob", false));
        room.accept(new Message.Insert("Created.java", 0, "class C {}", 0, "eve"));

        assertEquals("Here\nAda\nAda: hello", room.text());
    }

    @Test
    void theProjectRoomDoesNotChangeTheFileTree() throws Exception {
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
                        List<String> files = names(window);
                        window.applyRemote(List.of(
                                new Message.Presence("Ada", true),
                                new Message.Presence("Bob", true),
                                new Message.Chat("Ada", "hello"),
                                new Message.Presence("Bob", false),
                                new Message.Insert("Created.java", 0, "class C {}", 0, "eve")));
                        Platform.runLater(() -> {
                            try {
                                assertEquals("Here\nAda\nAda: hello", window.roomText());
                                assertEquals(files, names(window));
                                assertEquals(List.of("A.java"), files);
                                assertFalse(Files.exists(root.resolve("Created.java")));
                                assertFalse(Files.exists(root.resolve("Ada")));
                                stage.close();
                                window.close();
                            } catch (Throwable thrown) {
                                failure.set(thrown);
                            } finally {
                                done.countDown();
                            }
                        });
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

        assertTrue(done.await(15, TimeUnit.SECONDS));
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
    }

    private static List<String> names(IdeWindow window) {
        return window.listedRoot().getChildren().stream().map(item -> item.getValue().name()).toList();
    }
}
