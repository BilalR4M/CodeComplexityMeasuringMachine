package dev.bilal.ccmm.app.bench;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class KeystrokeBenchmarkFileTest {
    @Test
    void committedReportHasTheDecision() throws Exception {
        Path report = KeystrokeReport.reportFile(Path.of("").toAbsolutePath());
        String text = Files.readString(report);
        assertTrue(text.contains("p95:"), text);
        assertTrue(text.toLowerCase().contains("memory"), text);
        String verdict = text.strip().substring(text.strip().lastIndexOf('\n') + 1);
        assertTrue(verdict.equals("go") || verdict.equals("no-go"), verdict);
        if (verdict.equals("no-go")) {
            assertTrue(text.contains("P3-T02 is replaced with an editor change"));
        }
    }
}
