package dev.bilal.ccmm.core;

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;

/** Local measurement for docs/benchmarks/project-scan.md. Not a Surefire test. */
public final class ProjectScanBenchmark {
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("ccmm-scan-bench");
        int count = 300;
        for (int i = 0; i < count; i++) {
            Path dir = root.resolve("p" + (i % 20));
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("C" + i + ".java"), "class C" + i + " { int m() { return " + i + "; } }");
        }
        Analyzer analyzer = new Analyzer();
        long start = System.nanoTime();
        ProjectReport report = analyzer.scan(root);
        double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;
        long committed = ((OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean())
                .getCommittedVirtualMemorySize();
        long ok = report.files().stream().filter(result -> result instanceof AnalysisResult.Ok).count();
        System.out.printf("files=%d ok=%d ms=%.3f committedBytes=%d%n", count, ok, elapsedMs, committed);
        try (var walk = Files.walk(root)) {
            walk.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (java.io.IOException ignored) {
                    // best-effort cleanup of the temp tree
                }
            });
        }
    }
}
