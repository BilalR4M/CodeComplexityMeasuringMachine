package dev.bilal.ccmm.spikes.e0t02;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Latest-wins highlighting. A newer revision discards spans from an older job.
 * Mirrors AnalysisScheduler cancellation without depending on ccmm-app.
 */
public final class HighlightScheduler implements AutoCloseable {
    private final Highlighter highlighter;
    private final ExecutorService workers = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "e0-t02-highlight");
        t.setDaemon(true);
        return t;
    });
    private final AtomicLong revision = new AtomicLong();
    private Future<?> inFlight;

    public HighlightScheduler(Highlighter highlighter) {
        this.highlighter = highlighter;
    }

    public long submit(String text, String extension, Consumer<List<Span>> onReady) {
        long mine = revision.incrementAndGet();
        if (inFlight != null) {
            inFlight.cancel(true);
        }
        inFlight = workers.submit(() -> {
            if (mine != revision.get() || Thread.currentThread().isInterrupted()) {
                return;
            }
            List<Span> spans = highlighter.highlight(text, extension);
            if (mine != revision.get() || Thread.currentThread().isInterrupted()) {
                return;
            }
            onReady.accept(spans);
        });
        return mine;
    }

    public long currentRevision() {
        return revision.get();
    }

    @Override
    public void close() {
        workers.shutdownNow();
    }
}
