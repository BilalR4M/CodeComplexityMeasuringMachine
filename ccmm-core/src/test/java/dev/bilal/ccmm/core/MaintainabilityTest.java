package dev.bilal.ccmm.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MaintainabilityTest {
    private final Analyzer analyzer = new Analyzer();

    @Test
    void indexUsesNaturalLogAndClampsAtZero() {
        double volume = Math.E;
        double expected = (171 - 5.2 * Math.log(volume) - 0.23 * 1 - 16.2 * Math.log(4)) * 100 / 171;
        assertEquals(expected, Maintainability.index(volume, 1, 4), 1e-9);
        assertEquals(0, Maintainability.index(volume, 10_000, 4), 0);
        assertEquals(0, Maintainability.index(0, 1, 4), 0);
        assertEquals(0, Maintainability.index(volume, 1, 0), 0);
    }

    @Test
    void fileAndMethodUseTheSameVolumeComplexityAndCodeLines() {
        String source = """
                class A {
                    int m() {
                        // note
                        return 1 + 2;
                    }
                }
                """;
        FileMetrics file = metrics(source);
        MethodMetrics method = file.methods().getFirst();

        assertEquals(5, file.codeLines());
        assertEquals(
                Maintainability.index(file.halstead().volume(), file.cyclomatic(), file.codeLines()),
                file.maintainability(),
                1e-9);
        assertEquals(
                Maintainability.index(method.halstead().volume(), method.cyclomatic(), 3),
                method.maintainability(),
                1e-9);
        assertTrue(method.maintainability() > 0);
    }

    private FileMetrics metrics(String source) {
        AnalysisResult.Ok ok = assertInstanceOf(AnalysisResult.Ok.class, analyzer.analyze(source, "A.java"));
        return ok.file();
    }
}
