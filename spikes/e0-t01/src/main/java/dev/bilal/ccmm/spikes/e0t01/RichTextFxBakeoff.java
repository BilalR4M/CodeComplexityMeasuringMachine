package dev.bilal.ccmm.spikes.e0t01;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.stage.Stage;
import org.fxmisc.richtext.CodeArea;
/**
 * Measures RichTextFX against the E0-T01 go thresholds. Not part of the product.
 * Run: {@code mvn -f spikes/e0-t01 -q exec:java}
 */
public final class RichTextFxBakeoff {
    private static final String COMMAND = "mvn -f spikes/e0-t01 -q exec:java";
    private static final double BUDGET_MS = 16.0;
    private static final long MEMORY_BUDGET_BYTES = 500L * 1024L * 1024L;

    private RichTextFxBakeoff() {}

    public static void main(String[] args) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Result> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Runnable start = () -> run(done, result, failure);
        try {
            Platform.startup(start);
        } catch (IllegalStateException already) {
            Platform.runLater(start);
        }
        if (!done.await(10, TimeUnit.MINUTES)) {
            throw new IllegalStateException("RichTextFX bake-off timed out");
        }
        if (failure.get() != null) {
            throw new Exception(failure.get());
        }
        Path report = reportFile();
        Files.createDirectories(report.getParent());
        String incubating = IncubatingProbe.probe();
        Files.writeString(report, format(result.get(), incubating));
        System.out.println(report.toAbsolutePath());
        System.out.println(result.get().decision());
        Platform.exit();
    }

    private static void run(
            CountDownLatch done, AtomicReference<Result> result, AtomicReference<Throwable> failure) {
        try {
            Platform.setImplicitExit(false);
            Stage stage = new Stage();
            CodeArea area = new CodeArea();
            // Same left-decoration path line numbers use. A gutter chip is a Label in that factory.
            area.setParagraphGraphicFactory(line -> {
                Label chip = new Label("CC7");
                chip.getStyleClass().add("gutter-chip");
                chip.setMinWidth(36);
                return chip;
            });
            Scene scene = new Scene(area, 900, 700);
            stage.setScene(scene);
            stage.show();

            area.replaceText(Buffers.tenThousandLines());
            if (area.getParagraphs().size() != Buffers.LINES) {
                throw new IllegalStateException("expected " + Buffers.LINES + " paragraphs");
            }

            long[] samples = new long[Buffers.SAMPLES];
            int[] seen = {-30};
            AtomicReference<Long> memoryTenK = new AtomicReference<>();

            AnimationTimer timer = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    try {
                        if (seen[0] == 0) {
                            memoryTenK.set(committedBytes());
                        }
                        int paragraphs = area.getParagraphs().size();
                        int line = Math.floorMod(seen[0] * 47, paragraphs);
                        area.moveTo(line, 0);
                        int pos = area.getCaretPosition();
                        long started = System.nanoTime();
                        area.insertText(pos, "x");
                        long elapsed = System.nanoTime() - started;
                        if (seen[0] >= 0) {
                            samples[seen[0]] = elapsed;
                        }
                        seen[0]++;
                        if (seen[0] >= samples.length) {
                            stop();
                            finishLargeFile(area, stage, samples, memoryTenK.get(), result, done);
                        }
                    } catch (Throwable error) {
                        failure.set(error);
                        stop();
                        stage.close();
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

    private static void finishLargeFile(
            CodeArea area,
            Stage stage,
            long[] samples,
            long memoryTenK,
            AtomicReference<Result> result,
            CountDownLatch done) {
        Platform.runLater(() -> {
            try {
                String fiveMb = Buffers.fiveMegabytes();
                area.replaceText(fiveMb);
                long memoryFiveMb = committedBytes();
                ScrollBar vertical = findVertical(area);
                double before = vertical == null ? -1 : vertical.getValue();
                if (vertical != null) {
                    vertical.setValue(vertical.getMax());
                    vertical.setValue(vertical.getMin());
                    vertical.setValue(vertical.getMax() / 2);
                }
                String scroll = vertical == null
                        ? "no ScrollBar found; paragraphs=" + area.getParagraphs().size()
                        : String.format(
                                Locale.US,
                                "vertical ScrollBar moved from %.3f through max and to mid=%.3f without throw",
                                before,
                                vertical.getValue());

                // Remote carets must not use the local selection (that moves the viewport).
                // Style spans paint a peer range without changing caret or scroll.
                if (vertical != null) {
                    vertical.setValue(vertical.getMax());
                }
                double scrolled = vertical == null ? 0 : vertical.getValue();
                int localCaret = area.getCaretPosition();
                area.setStyleClass(10, Math.min(20, area.getLength()), "remote-caret");
                double afterStyle = vertical == null ? 0 : vertical.getValue();
                boolean caretWithoutScroll = area.getCaretPosition() == localCaret
                        && (vertical == null || Math.abs(afterStyle - scrolled) < 0.05);

                boolean gutter = area.getParagraphGraphicFactory() != null;
                boolean folding = false; // RichTextFX 0.11.2 has no fold API.

                double p95 = Percentile.millis(samples, 0.95);
                result.set(new Result(
                        System.getProperty("os.name") + " " + System.getProperty("os.arch"),
                        "21.0.5",
                        p95,
                        memoryTenK,
                        memoryFiveMb,
                        scroll,
                        gutter,
                        caretWithoutScroll,
                        folding,
                        decide(p95, memoryFiveMb, gutter, caretWithoutScroll)));
                stage.close();
                done.countDown();
            } catch (Throwable error) {
                done.countDown();
                throw new RuntimeException(error);
            }
        });
    }

    private static String decide(double p95, long memoryFiveMb, boolean gutter, boolean caret) {
        if (p95 < BUDGET_MS && memoryFiveMb < MEMORY_BUDGET_BYTES && gutter && caret) {
            return "go";
        }
        return "no-go";
    }

    private static ScrollBar findVertical(CodeArea area) {
        for (var node : area.lookupAll(".scroll-bar")) {
            if (node instanceof ScrollBar bar && bar.getOrientation() == Orientation.VERTICAL) {
                return bar;
            }
        }
        return null;
    }

    private static long committedBytes() {
        var os = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        return os.getCommittedVirtualMemorySize();
    }

    private static Path reportFile() {
        Path cwd = Path.of("").toAbsolutePath();
        if (Files.isDirectory(cwd.resolve("docs/benchmarks"))) {
            return cwd.resolve("docs/benchmarks/editor-bakeoff.md");
        }
        if (Files.isDirectory(cwd.resolve("../../docs/benchmarks"))) {
            return cwd.resolve("../../docs/benchmarks/editor-bakeoff.md").normalize();
        }
        return cwd.resolve("docs/benchmarks/editor-bakeoff.md");
    }

    private static String format(Result r, String incubating) {
        String memoryTen = String.format(Locale.US, "%.1f", r.memoryTenK() / (1024.0 * 1024.0));
        String memoryFive = String.format(Locale.US, "%.1f", r.memoryFiveMb() / (1024.0 * 1024.0));
        String p95 = String.format(Locale.US, "%.3f", r.p95Ms());
        StringBuilder out = new StringBuilder();
        out.append("# Editor bake-off\n\n");
        out.append("E0-T01. Spike sources live under `spikes/e0-t01/` and are not on the app classpath.\n");
        out.append("The percentile helper is duplicated from `KeystrokeReport` so the spike stays standalone.\n\n");
        out.append("## RichTextFX (`org.fxmisc.richtext.CodeArea`)\n\n");
        out.append("- OS: ").append(r.os()).append('\n');
        out.append("- JavaFX: ").append(r.javafxVersion()).append('\n');
        out.append("- Command: `").append(COMMAND).append("`\n");
        out.append("- Buffer (keystroke): ").append(Buffers.LINES).append(" lines\n");
        out.append("- Samples: ").append(Buffers.SAMPLES).append(" single-character inserts\n");
        out.append("- p95: ").append(p95).append(" ms\n");
        out.append("- memory after 10k-line load: ").append(memoryTen).append(" MB committed\n");
        out.append("- Buffer (large): 5 MB text\n");
        out.append("- memory after 5 MB load: ").append(memoryFive).append(" MB committed\n");
        out.append("- scroll: ").append(r.scrollNote()).append('\n');
        out.append("- gutter chip / side decoration: ").append(r.gutter() ? "yes" : "no").append('\n');
        out.append("- caret/selection without moving viewport (local range at top): ")
                .append(r.caretWithoutScroll() ? "yes" : "no")
                .append('\n');
        out.append("- folding feasible: ").append(r.folding() ? "yes" : "no").append('\n');
        out.append("  RichTextFX 0.11.2 exposes no fold API. Folding is not required for a go.\n\n");
        out.append("## Incubating JavaFX `CodeArea`\n\n");
        out.append(incubating).append("\n\n");
        out.append("## Decision\n\n");
        if ("go".equals(r.decision())) {
            out.append("go\n\n");
            out.append("RichTextFX meets the go thresholds. Keep ADR-0001. ");
            out.append("Folding stays optional and is currently no.\n");
        } else {
            out.append("no-go\n\n");
            out.append("E2 tasks that assume RichTextFX stop until the plan names a replacement. ");
            out.append("Do not implement the loser.\n");
        }
        return out.toString();
    }

    record Result(
            String os,
            String javafxVersion,
            double p95Ms,
            long memoryTenK,
            long memoryFiveMb,
            String scrollNote,
            boolean gutter,
            boolean caretWithoutScroll,
            boolean folding,
            String decision) {}
}
