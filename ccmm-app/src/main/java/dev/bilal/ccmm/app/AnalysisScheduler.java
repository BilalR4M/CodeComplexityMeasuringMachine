package dev.bilal.ccmm.app;

import dev.bilal.ccmm.core.AnalysisResult;
import dev.bilal.ccmm.core.Analyzer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Waits for a quiet period, then analyzes a buffer snapshot off the JavaFX thread.
 * A newer edit cancels the pending job and drops a result that is already in flight.
 */
final class AnalysisScheduler {
    static final long QUIET_MILLIS = 300;

    @FunctionalInterface
    interface AnalysisCall {
        AnalysisResult analyze(String source, String relativePath);
    }

    private final long delayMs;
    private final Consumer<Runnable> onFxThread;
    private final AnalysisCall call;
    private final ScheduledExecutorService worker;
    private final AtomicInteger generation = new AtomicInteger();
    private ScheduledFuture<?> pending;

    AnalysisScheduler(long delayMs, Consumer<Runnable> onFxThread) {
        this(delayMs, onFxThread, new Analyzer()::analyze);
    }

    AnalysisScheduler(long delayMs, Consumer<Runnable> onFxThread, AnalysisCall call) {
        this.delayMs = delayMs;
        this.onFxThread = onFxThread;
        this.call = call;
        this.worker = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "ccmm-analysis");
            thread.setDaemon(true);
            return thread;
        });
    }

    void schedule(String source, String relativePath, Consumer<AnalysisResult> onResult) {
        if (source == null || relativePath == null || onResult == null) {
            return;
        }
        int ticket = generation.incrementAndGet();
        if (pending != null) {
            pending.cancel(false);
        }
        pending = worker.schedule(() -> finish(ticket, source, relativePath, onResult), delayMs, TimeUnit.MILLISECONDS);
    }

    void close() {
        generation.incrementAndGet();
        if (pending != null) {
            pending.cancel(false);
        }
        worker.shutdown();
    }

    private void finish(int ticket, String source, String relativePath, Consumer<AnalysisResult> onResult) {
        if (ticket != generation.get()) {
            return;
        }
        AnalysisResult result = call.analyze(source, relativePath);
        onFxThread.accept(() -> {
            if (ticket != generation.get()) {
                return;
            }
            onResult.accept(result);
        });
    }
}
