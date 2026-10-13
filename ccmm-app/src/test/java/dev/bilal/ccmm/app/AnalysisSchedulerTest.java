package dev.bilal.ccmm.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.bilal.ccmm.core.AnalysisResult;
import dev.bilal.ccmm.core.Analyzer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class AnalysisSchedulerTest {
    @Test
    void anEditWaitsThenAnalyzesOffTheCallerThread() throws Exception {
        List<AnalysisResult> results = new ArrayList<>();
        AtomicReference<String> thread = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        AnalysisScheduler scheduler = new AnalysisScheduler(80, Runnable::run, (source, path) -> {
            thread.set(Thread.currentThread().getName());
            return new Analyzer().analyze(source, path);
        });
        long started = System.nanoTime();
        scheduler.schedule("class A {}", "A.java", result -> {
            results.add(result);
            done.countDown();
        });

        assertTrue(done.await(2, TimeUnit.SECONDS));
        double elapsedMs = (System.nanoTime() - started) / 1_000_000.0;
        assertTrue(elapsedMs >= 70, "elapsed " + elapsedMs);
        assertEquals("ccmm-analysis", thread.get());
        AnalysisResult.Ok ok = assertInstanceOf(AnalysisResult.Ok.class, results.get(0));
        assertEquals("A.java", ok.relativePath());
        scheduler.close();
    }

    @Test
    void aPendingEditIsCancelled() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        List<String> paths = new ArrayList<>();
        CountDownLatch done = new CountDownLatch(1);
        AnalysisScheduler scheduler = new AnalysisScheduler(50, Runnable::run, (source, path) -> {
            calls.incrementAndGet();
            return new Analyzer().analyze(source, path);
        });
        scheduler.schedule("class A {}", "A.java", result -> paths.add(pathOf(result)));
        scheduler.schedule("class B {}", "B.java", result -> {
            paths.add(pathOf(result));
            done.countDown();
        });

        assertTrue(done.await(2, TimeUnit.SECONDS));
        assertEquals(1, calls.get());
        assertEquals(List.of("B.java"), paths);
        scheduler.close();
    }

    @Test
    void aNewerEditDropsTheOlderResult() throws Exception {
        List<AnalysisResult> results = new ArrayList<>();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(1);
        AnalysisScheduler scheduler = new AnalysisScheduler(20, Runnable::run, (source, path) -> {
            if (source.contains("Slow")) {
                started.countDown();
                try {
                    if (!release.await(2, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("release timed out");
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(interrupted);
                }
            }
            return new Analyzer().analyze(source, path);
        });
        scheduler.schedule("class Slow {}", "Slow.java", results::add);
        assertTrue(started.await(2, TimeUnit.SECONDS));
        scheduler.schedule("class Fast {}", "Fast.java", result -> {
            results.add(result);
            done.countDown();
        });
        release.countDown();

        assertTrue(done.await(2, TimeUnit.SECONDS));
        assertEquals(1, results.size());
        assertEquals("Fast.java", pathOf(results.get(0)));
        scheduler.close();
    }

    @Test
    void aParseFailureIsPublished() throws Exception {
        List<AnalysisResult> results = new ArrayList<>();
        CountDownLatch done = new CountDownLatch(1);
        AnalysisScheduler scheduler = new AnalysisScheduler(20, Runnable::run);
        scheduler.schedule("class {", "Bad.java", result -> {
            results.add(result);
            done.countDown();
        });

        assertTrue(done.await(2, TimeUnit.SECONDS));
        assertInstanceOf(AnalysisResult.Failed.class, results.get(0));
        scheduler.close();
    }

    private static String pathOf(AnalysisResult result) {
        return switch (result) {
            case AnalysisResult.Ok ok -> ok.relativePath();
            case AnalysisResult.Failed failed -> failed.relativePath();
        };
    }
}
