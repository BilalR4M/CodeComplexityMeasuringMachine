package dev.bilal.ccmm.core;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProjectScanBenchmarkFileTest {
    @Test
    void committedReportIsAGo() throws Exception {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null && !Files.exists(dir.resolve("docs/benchmarks/project-scan.md"))) {
            dir = dir.getParent();
        }
        assertTrue(dir != null, "docs/benchmarks/project-scan.md");
        String text = Files.readString(dir.resolve("docs/benchmarks/project-scan.md"));
        assertTrue(text.contains("Duration:"), text);
        assertTrue(text.toLowerCase().contains("memory"), text);
        String verdict = text.strip().substring(text.strip().lastIndexOf('\n') + 1);
        assertTrue(verdict.equals("go") || verdict.equals("no-go"), verdict);
    }
}
