package dev.bilal.ccmm.app.editor;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Latest-wins highlighting off the FX thread (ADR-0009). Same shape as
 * AnalysisScheduler: a newer edit drops an in-flight result.
 */
public final class HighlightScheduler {
    private final RegexJavaHighlighter highlighter = new RegexJavaHighlighter();
    private final Consumer<Runnable> onFxThread;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "ccmm-highlight");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicInteger generation = new AtomicInteger();

    public HighlightScheduler(Consumer<Runnable> onFxThread) {
        this.onFxThread = onFxThread;
    }

    public void schedule(String text, String relativePath, Consumer<List<HighlightSpan>> onReady) {
        if (text == null || relativePath == null || onReady == null) {
            return;
        }
        int ticket = generation.incrementAndGet();
        worker.execute(() -> {
            if (ticket != generation.get()) {
                return;
            }
            List<HighlightSpan> spans;
            try {
                spans = highlighter.highlight(text, relativePath);
            } catch (RuntimeException failure) {
                return; // leave previous spans
            }
            if (ticket != generation.get()) {
                return;
            }
            onFxThread.accept(() -> {
                if (ticket != generation.get()) {
                    return;
                }
                onReady.accept(spans);
            });
        });
    }

    public void close() {
        generation.incrementAndGet();
        worker.shutdownNow();
    }
}
