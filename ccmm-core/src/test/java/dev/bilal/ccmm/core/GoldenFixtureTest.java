package dev.bilal.ccmm.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class GoldenFixtureTest {
    @Test
    void sampleMatchesTheAdr0004Metrics() throws Exception {
        Path root = Path.of("").toAbsolutePath();
        if (!Files.exists(root.resolve("fixtures"))) {
            root = root.getParent();
        }
        Path legacy = root.resolve("fixtures/legacy/sampleCode.txt");
        Path source = root.resolve("fixtures/golden/sampleCode.txt");
        Path expected = root.resolve("fixtures/golden/sampleCode.metrics");
        String text = Files.readString(source).replace("\r\n", "\n").replace('\r', '\n');

        assertEquals(Files.readString(legacy).replace("\r\n", "\n").replace('\r', '\n'), text);
        AnalysisResult.Ok ok = assertInstanceOf(
                AnalysisResult.Ok.class, new Analyzer().analyze(text, "fixtures/golden/sampleCode.txt"));
        String actual = GoldenReport.format(ok.file());
        String oracle = Files.readString(expected).replace("\r\n", "\n").replace('\r', '\n');
        assertEquals(oracle, actual);
    }
}
