package dev.bilal.ccmm.app.bench;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MetricsRefreshBenchmarkFileTest {
    @Test
    void committedReportHasTheBudget() throws Exception {
        Path report = MetricsRefreshHarness.reportFile(Path.of("").toAbsolutePath());
        String text = Files.readString(report);
        assertTrue(text.contains("Refresh:"), text);
        assertTrue(text.contains("300 ms"), text);
        String verdict = text.strip().substring(text.strip().lastIndexOf('\n') + 1);
        assertTrue(verdict.equals("go") || verdict.equals("no-go"), verdict);
    }

    @Test
    void aRefreshInsideTwiceTheQuietTimeIsAGo() {
        String report = MetricsRefreshHarness.format("Windows 11 amd64", 320);
        assertTrue(report.contains("Refresh: 320.000 ms"));
        assertTrue(report.contains("Budget: 300 ms"));
        assertTrue(report.strip().endsWith("go"));
    }

    @Test
    void aSlowRefreshIsANoGo() {
        String report = MetricsRefreshHarness.format("Windows 11 amd64", 700);
        assertTrue(report.strip().endsWith("no-go"));
    }
}
