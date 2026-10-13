package dev.bilal.ccmm.app.bench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class KeystrokeReportTest {
    @Test
    void sampleHasTenThousandLines() {
        String text = KeystrokeReport.sample(KeystrokeReport.LINES);
        assertEquals(KeystrokeReport.LINES, text.split("\n", -1).length);
    }

    @Test
    void p95IsTheNearestRankAboveThePercentile() {
        long[] nanos = new long[10];
        for (int i = 0; i < nanos.length; i++) {
            nanos[i] = (i + 1) * 1_000_000L;
        }
        assertEquals(10.0, KeystrokeReport.percentileMillis(nanos, 0.95));
    }

    @Test
    void reportEndsWithGoUnderTheBudget() {
        String report = KeystrokeReport.format("Windows amd64", "mvn test", 1.25, 200L * 1024 * 1024);
        assertTrue(report.contains("p95: 1.250 ms"));
        assertTrue(report.contains("memory: 200.0 MB committed"));
        assertTrue(report.strip().endsWith("go"));
    }

    @Test
    void reportEndsWithNoGoAndBlocksTheEditorTask() {
        String report = KeystrokeReport.format("Windows amd64", "mvn test", 20.0, 1024 * 1024);
        assertTrue(report.contains("P3-T02 is replaced with an editor change"));
        assertTrue(report.strip().endsWith("no-go"));
    }

    @Test
    void anEditorMeasurementIsAppendedAfterTheSpike() {
        String spike = KeystrokeReport.format("Windows amd64", "mvn test", 1.25, 200L * 1024 * 1024);
        String combined = KeystrokeReport.withEditorMeasurement(spike, "Windows amd64", "mvn test", 2.5, 300L * 1024 * 1024);

        assertTrue(combined.contains("p95: 1.250 ms"));
        assertTrue(combined.contains("## Editor"));
        assertTrue(combined.contains("p95: 2.500 ms"));
        assertTrue(combined.strip().endsWith("go"));
    }
}
