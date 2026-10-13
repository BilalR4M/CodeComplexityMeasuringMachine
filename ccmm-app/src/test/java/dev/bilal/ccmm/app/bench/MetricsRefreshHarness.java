package dev.bilal.ccmm.app.bench;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.bilal.ccmm.app.FileNode;
import dev.bilal.ccmm.app.IdeWindow;
import java.awt.GraphicsEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import javafx.stage.Stage;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** Local measurement. CI skips it. Run with {@code CCMM_METRICS_REFRESH=true}. */
class MetricsRefreshHarness {
    static final String COMMAND =
            "CCMM_METRICS_REFRESH=true mvn -B -ntp -pl ccmm-app -am test -Dtest=MetricsRefreshHarness";
    static final long QUIET_MILLIS = 300;

    @Test
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void measuresQuietTimeUntilTheResultIsApplied() throws Exception {
        Assumptions.assumeTrue("true".equalsIgnoreCase(System.getenv("CCMM_METRICS_REFRESH")));
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());

        Path project = Files.createTempDirectory("ccmm-metrics-refresh");
        Files.writeString(project.resolve("Sample.java"), "class Sample {}");

        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Double> refreshMs = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<Long> started = new AtomicReference<>();
        Runnable start = () -> {
            try {
                Platform.setImplicitExit(false);
                IdeWindow window = new IdeWindow();
                Stage stage = new Stage();
                stage.setTitle("CCMM IDE");
                stage.setScene(window.scene());
                window.setOnListed(() -> {
                    TreeItem<FileNode> sample = window.listedRoot().getChildren().stream()
                            .filter(item -> item.getValue().name().equals("Sample.java"))
                            .findFirst()
                            .orElseThrow();
                    window.select(sample);
                });
                window.setOnOpened(() -> {
                    window.setOnAnalyzed(() -> {
                        refreshMs.set((System.nanoTime() - started.get()) / 1_000_000.0);
                        stage.close();
                        window.close();
                        done.countDown();
                    });
                    started.set(System.nanoTime());
                    window.editor().insertText(window.editor().getLength(), " ");
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
        Files.writeString(reportFile(Path.of("").toAbsolutePath()), format(os, refreshMs.get()));
    }

    static String format(String os, double refreshMs) {
        String refresh = String.format(Locale.US, "%.3f", refreshMs);
        String verdict = refreshMs >= QUIET_MILLIS - 50 && refreshMs < QUIET_MILLIS * 2 ? "go" : "no-go";
        return """
                # Metrics refresh

                Refresh is the time from the last edit until that buffer's analysis result is applied on the JavaFX thread. The quiet time is 300 ms. Parsing runs after that, off the JavaFX thread.

                - OS: %s
                - Command: `%s`
                - Quiet time: 300 ms
                - Refresh: %s ms
                - Budget: 300 ms

                %s
                """.formatted(os, COMMAND, refresh, verdict);
    }

    static Path reportFile(Path cwd) {
        if (Files.exists(cwd.resolve("docs/benchmarks"))) {
            return cwd.resolve("docs/benchmarks/metrics-refresh.md");
        }
        return cwd.getParent().resolve("docs/benchmarks/metrics-refresh.md");
    }
}
