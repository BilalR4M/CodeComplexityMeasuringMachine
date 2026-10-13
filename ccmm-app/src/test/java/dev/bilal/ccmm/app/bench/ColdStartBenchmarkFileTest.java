package dev.bilal.ccmm.app.bench;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ColdStartBenchmarkFileTest {
    @Test
    void committedReportIsAGo() throws Exception {
        Path report = ColdStartHarness.reportFile(Path.of("").toAbsolutePath());
        String text = Files.readString(report);
        assertTrue(text.contains("Duration:"), text);
        assertTrue(text.contains("2000 ms"), text);
        String verdict = text.strip().substring(text.strip().lastIndexOf('\n') + 1);
        assertTrue(verdict.equals("go") || verdict.equals("no-go"), verdict);
    }
}
