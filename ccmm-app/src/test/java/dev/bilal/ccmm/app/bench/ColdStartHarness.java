package dev.bilal.ccmm.app.bench;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import dev.bilal.ccmm.app.IdeWindow;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** Local measurement. CI skips it. Run with {@code CCMM_COLD_START=true}. */
class ColdStartHarness {
    static final String COMMAND =
            "CCMM_COLD_START=true mvn -B -ntp -pl ccmm-app -am test -Dtest=ColdStartHarness";

    @Test
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void measuresTimeToAUsableWindow() throws Exception {
        Assumptions.assumeTrue("true".equalsIgnoreCase(System.getenv("CCMM_COLD_START")));
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());

        Path project = Files.createTempDirectory("ccmm-cold-start");
        int count = 50;
        for (int i = 0; i < count; i++) {
            Files.writeString(project.resolve("F" + i + ".java"), "class F" + i + " {}");
        }

        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Double> elapsedMs = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        long started = System.nanoTime();
        Runnable start = () -> {
            try {
                IdeWindow window = new IdeWindow();
                Stage stage = new Stage();
                stage.setTitle("CCMM IDE");
                stage.setScene(window.scene());
                window.setOnListed(() -> {
                    elapsedMs.set((System.nanoTime() - started) / 1_000_000.0);
                    stage.close();
                    window.close();
                    done.countDown();
                });
                stage.show();
                window.open(project);
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

        assertTrue(done.await(30, TimeUnit.SECONDS));
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
        String os = System.getProperty("os.name") + " " + System.getProperty("os.arch");
        Files.writeString(reportFile(Path.of("").toAbsolutePath()), format(os, count, elapsedMs.get()));
    }

    static String format(String os, int files, double elapsedMs) {
        String verdict = elapsedMs < 2000 ? "go" : "no-go";
        return """
                # Cold start

                Duration is the time from JavaFX startup until the stage is showing and the file tree is filled. It does not include Maven startup.

                - OS: %s
                - Command: `%s`
                - Files: %d
                - Duration: %.3f ms
                - Budget: 2000 ms

                %s
                """.formatted(os, COMMAND, files, elapsedMs, verdict);
    }

    static Path reportFile(Path cwd) {
        if (Files.exists(cwd.resolve("docs/benchmarks"))) {
            return cwd.resolve("docs/benchmarks/cold-start.md");
        }
        return cwd.getParent().resolve("docs/benchmarks/cold-start.md");
    }
}
