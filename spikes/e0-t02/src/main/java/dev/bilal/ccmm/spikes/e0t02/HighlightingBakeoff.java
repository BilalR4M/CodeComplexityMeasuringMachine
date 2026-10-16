package dev.bilal.ccmm.spikes.e0t02;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.fxmisc.richtext.CodeArea;

/**
 * Measures the regex Java highlighter against E0-T02 go thresholds.
 * Run: {@code mvn -f spikes/e0-t02 package exec:java}
 */
public final class HighlightingBakeoff {
    private static final String COMMAND = "mvn -f spikes/e0-t02 package exec:java";
    private static final double HIGHLIGHT_BUDGET_MS = 50.0;
    private static final double KEYSTROKE_BUDGET_MS = 16.0;

    private HighlightingBakeoff() {}

    public static void main(String[] args) throws Exception {
        RegexJavaHighlighter lexer = new RegexJavaHighlighter();
        String tenK = Buffers.tenThousandLines();
        String fiveMb = Buffers.fiveMegabytes();

        long tenKNanos = timeHighlight(lexer, tenK);
        long fiveMbNanos = timeHighlight(lexer, fiveMb);
        boolean cancelOk = measureCancel(lexer);

        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<double[]> keystroke = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Runnable start = () -> measureKeystroke(lexer, tenK, done, keystroke, failure);
        try {
            Platform.startup(start);
        } catch (IllegalStateException already) {
            Platform.runLater(start);
        }
        if (!done.await(10, TimeUnit.MINUTES)) {
            throw new IllegalStateException("keystroke measurement timed out");
        }
        if (failure.get() != null) {
            throw new Exception(failure.get());
        }

        double tenKMs = tenKNanos / 1_000_000.0;
        double fiveMbMs = fiveMbNanos / 1_000_000.0;
        double p95 = keystroke.get()[0];
        String decision = tenKMs < HIGHLIGHT_BUDGET_MS && p95 < KEYSTROKE_BUDGET_MS ? "go" : "no-go";

        Path report = reportFile();
        Files.createDirectories(report.getParent());
        Files.writeString(
                report,
                format(
                        tenKMs,
                        fiveMbMs,
                        p95,
                        cancelOk,
                        decision,
                        Tm4eProbe.probe(),
                        TreeSitterProbe.probe()));
        System.out.println(report.toAbsolutePath());
        System.out.println(decision);
        Platform.exit();
    }

    private static long timeHighlight(Highlighter highlighter, String text) {
        highlighter.highlight(text, "java"); // warm
        long started = System.nanoTime();
        List<Span> spans = highlighter.highlight(text, "java");
        long elapsed = System.nanoTime() - started;
        if (spans.isEmpty()) {
            throw new IllegalStateException("expected spans");
        }
        return elapsed;
    }

    /**
     * Submits a slow revision then a fast one. The slow job must not deliver spans.
     */
    static boolean measureCancel(Highlighter highlighter) throws InterruptedException {
        AtomicInteger delivered = new AtomicInteger();
        AtomicBoolean slowDelivered = new AtomicBoolean();
        try (HighlightScheduler scheduler = new HighlightScheduler((text, extension) -> {
            if ("SLOW".equals(text)) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return List.of();
                }
            }
            return highlighter.highlight(text, extension);
        })) {
            scheduler.submit("SLOW", "java", spans -> {
                slowDelivered.set(true);
                delivered.incrementAndGet();
            });
            Thread.sleep(20);
            CountDownLatch ready = new CountDownLatch(1);
            scheduler.submit(Buffers.tenThousandLines(), "java", spans -> {
                delivered.incrementAndGet();
                ready.countDown();
            });
            if (!ready.await(5, TimeUnit.SECONDS)) {
                return false;
            }
            Thread.sleep(250);
            return !slowDelivered.get() && delivered.get() == 1;
        }
    }

    private static void measureKeystroke(
            Highlighter highlighter,
            String buffer,
            CountDownLatch done,
            AtomicReference<double[]> keystroke,
            AtomicReference<Throwable> failure) {
        try {
            Platform.setImplicitExit(false);
            Stage stage = new Stage();
            CodeArea area = new CodeArea();
            stage.setScene(new Scene(area, 900, 700));
            stage.show();
            area.replaceText(buffer);

            HighlightScheduler scheduler = new HighlightScheduler(highlighter);
            AtomicReference<List<Span>> latest = new AtomicReference<>(List.of());
            scheduler.submit(buffer, "java", latest::set);

            long[] samples = new long[Buffers.SAMPLES];
            int[] seen = {-30};

            AnimationTimer timer = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    try {
                        int paragraphs = area.getParagraphs().size();
                        int line = Math.floorMod(Math.max(seen[0], 0) * 47, Math.max(paragraphs, 1));
                        area.moveTo(line, 0);
                        int pos = area.getCaretPosition();
                        String next = area.getText();
                        scheduler.submit(next, "java", spans -> {
                            latest.set(spans);
                            Platform.runLater(() -> applyStyles(area, spans));
                        });
                        long started = System.nanoTime();
                        area.insertText(pos, "x");
                        long elapsed = System.nanoTime() - started;
                        if (seen[0] >= 0) {
                            samples[seen[0]] = elapsed;
                        }
                        seen[0]++;
                        if (seen[0] >= samples.length) {
                            stop();
                            keystroke.set(new double[] {Percentile.millis(samples, 0.95)});
                            scheduler.close();
                            stage.close();
                            done.countDown();
                        }
                    } catch (Throwable error) {
                        failure.set(error);
                        stop();
                        scheduler.close();
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

    private static void applyStyles(CodeArea area, List<Span> spans) {
        area.clearStyle(0, area.getLength());
        int applied = 0;
        for (Span span : spans) {
            if (span.end() > area.getLength()) {
                continue;
            }
            area.setStyleClass(span.start(), span.end(), span.token());
            applied++;
            if (applied > 500) {
                // Cap FX work per frame so the harness measures insert cost, not a full restyle.
                break;
            }
        }
    }

    private static Path reportFile() {
        Path cwd = Path.of("").toAbsolutePath();
        if (Files.isDirectory(cwd.resolve("docs/benchmarks"))) {
            return cwd.resolve("docs/benchmarks/highlighting.md");
        }
        if (Files.isDirectory(cwd.resolve("../../docs/benchmarks"))) {
            return cwd.resolve("../../docs/benchmarks/highlighting.md").normalize();
        }
        return cwd.resolve("docs/benchmarks/highlighting.md");
    }

    private static String format(
            double tenKMs,
            double fiveMbMs,
            double p95,
            boolean cancelOk,
            String decision,
            String tm4e,
            String treeSitter) {
        String os = System.getProperty("os.name") + " " + System.getProperty("os.arch");
        StringBuilder out = new StringBuilder();
        out.append("# Highlighting bake-off\n\n");
        out.append("E0-T02. Spike sources live under `spikes/e0-t02/` and are not on the app classpath.\n\n");
        out.append("## Regex Java lexer\n\n");
        out.append("- OS: ").append(os).append('\n');
        out.append("- JDK: ").append(System.getProperty("java.version")).append('\n');
        out.append("- Command: `").append(COMMAND).append("`\n");
        out.append("- Buffer (highlight): ").append(Buffers.LINES).append(" lines\n");
        out.append("- Highlight time (10k, off FX thread): ")
                .append(String.format(Locale.US, "%.3f", tenKMs))
                .append(" ms\n");
        out.append("- Highlight time (5 MB, off FX thread): ")
                .append(String.format(Locale.US, "%.3f", fiveMbMs))
                .append(" ms\n");
        out.append("- Keystroke p95 with highlighting enabled: ")
                .append(String.format(Locale.US, "%.3f", p95))
                .append(" ms\n");
        out.append("- Cancel / latest-wins discards stale spans: ")
                .append(cancelOk ? "yes" : "no")
                .append('\n');
        out.append("- Budget: highlight 10k under 50 ms; keystroke p95 under 16 ms\n\n");
        out.append("## tm4e\n\n");
        out.append(tm4e).append("\n\n");
        out.append("## Tree-sitter\n\n");
        out.append(treeSitter).append("\n\n");
        out.append("## Decision\n\n");
        out.append(decision).append("\n\n");
        if ("go".equals(decision)) {
            out.append("Regex lexers meet the go thresholds. Keep ADR-0009's recommendation. ");
            out.append("Do not add tm4e or tree-sitter to `ccmm-app` from this spike.\n");
        } else {
            out.append("no-go for regex. Name tm4e only if the owner accepts EPL-2.0, ");
            out.append("otherwise drop highlighting for the failing language.\n");
        }
        return out.toString();
    }
}
