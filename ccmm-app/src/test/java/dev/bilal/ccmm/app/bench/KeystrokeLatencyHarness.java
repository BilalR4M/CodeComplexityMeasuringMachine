package dev.bilal.ccmm.app.bench;

import dev.bilal.ccmm.app.IdeWindow;
import java.awt.GraphicsEnvironment;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.fxmisc.richtext.CodeArea;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Local measurement. CI skips it. Run with {@code CCMM_KEYSTROKE=true}.
 * The numbers land in {@code docs/benchmarks/keystroke-latency.md}.
 */
class KeystrokeLatencyHarness {
    private static final String COMMAND =
            "CCMM_KEYSTROKE=true mvn -B -ntp -pl ccmm-app -am test -Dtest=KeystrokeLatencyHarness,KeystrokeReportTest";

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void measuresTenThousandLineBuffer() throws Exception {
        Assumptions.assumeTrue("true".equalsIgnoreCase(System.getenv("CCMM_KEYSTROKE")));
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());

        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<long[]> samples = new AtomicReference<>();
        AtomicReference<Long> memory = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Runnable start = () -> measure(done, samples, memory, failure);
        try {
            Platform.startup(start);
        } catch (IllegalStateException alreadyRunning) {
            Platform.runLater(start);
        }

        assertTrueAwait(done);
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }

        double p95 = KeystrokeReport.percentileMillis(samples.get(), 0.95);
        String os = System.getProperty("os.name") + " " + System.getProperty("os.arch");
        java.nio.file.Path report = KeystrokeReport.reportFile(java.nio.file.Path.of("").toAbsolutePath());
        String existing = Files.readString(report);
        Files.writeString(report, KeystrokeReport.withEditorMeasurement(existing, os, COMMAND, p95, memory.get()));
    }

    private static void assertTrueAwait(CountDownLatch done) throws InterruptedException {
        if (!done.await(2, TimeUnit.MINUTES)) {
            throw new AssertionError("keystroke harness timed out");
        }
    }

    private static void measure(
            CountDownLatch done,
            AtomicReference<long[]> samples,
            AtomicReference<Long> memory,
            AtomicReference<Throwable> failure) {
        try {
            Platform.setImplicitExit(false);
            IdeWindow window = new IdeWindow();
            Stage stage = new Stage();
            stage.setTitle("CCMM IDE");
            stage.setScene(window.scene());
            stage.show();
            CodeArea area = window.editor();
            area.replaceText(KeystrokeReport.sample(KeystrokeReport.LINES));

            long[] measured = new long[KeystrokeReport.SAMPLES];
            int[] seen = {-30};

            AnimationTimer timer = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    try {
                        if (seen[0] == -30 && area.getParagraphs().size() != KeystrokeReport.LINES) {
                            throw new IllegalStateException(
                                    "expected " + KeystrokeReport.LINES + " paragraphs, was " + area.getParagraphs().size());
                        }
                        if (seen[0] == 0) {
                            memory.set(committedBytes());
                        }
                        int paragraphs = area.getParagraphs().size();
                        int line = Math.floorMod(seen[0] * 47, paragraphs);
                        area.moveTo(line, 0);
                        int pos = area.getCaretPosition();
                        long started = System.nanoTime();
                        area.insertText(pos, "x");
                        long elapsed = System.nanoTime() - started;
                        if (seen[0] >= 0) {
                            measured[seen[0]] = elapsed;
                        }
                        seen[0]++;
                        if (seen[0] >= measured.length) {
                            stop();
                            samples.set(measured);
                            stage.close();
                            window.close();
                            done.countDown();
                        }
                    } catch (Throwable error) {
                        failure.set(error);
                        stop();
                        stage.close();
                        window.close();
                        done.countDown();
                    }
                }
            };
            timer.start();
        } catch (Throwable error) {
            failure.set(error);
            done.countDown();
        }
    }

    private static long committedBytes() {
        var os = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        return os.getCommittedVirtualMemorySize();
    }
}
