package dev.bilal.ccmm.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;

class HalsteadTest {
    private final Analyzer analyzer = new Analyzer();

    @Test
    void operatorsAndLiteralsComeFromTheAst() {
        Halstead halstead = methodHalstead("int m() { return 1 + 2; }");

        assertEquals(2, halstead.uniqueOperators());
        assertEquals(2, halstead.totalOperators());
        assertEquals(3, halstead.uniqueOperands());
        assertEquals(3, halstead.totalOperands());
        assertEquals(5, halstead.vocabulary());
        assertEquals(5, halstead.length());
        double volume = 5 * (Math.log(5) / Math.log(2));
        assertEquals(volume, halstead.volume(), 1e-9);
        assertEquals(1, halstead.difficulty(), 1e-9);
        assertEquals(volume, halstead.effort(), 1e-9);
    }

    @Test
    void keywordsAreNotOperandsAndStringLiteralsAre() {
        Halstead primitive = methodHalstead("int m() { return 1; }");
        Halstead text = methodHalstead("String m() { return \"ab\"; }");

        assertEquals(2, primitive.uniqueOperands());
        assertEquals(3, text.uniqueOperands());
        assertEquals(1, text.totalOperators());
    }

    @Test
    void repeatedLiteralCountsOnceTowardVocabulary() {
        Halstead halstead = methodHalstead("int m() { return 1 + 1; }");

        assertEquals(2, halstead.uniqueOperands());
        assertEquals(3, halstead.totalOperands());
    }

    @Test
    void incrementIsOneOperator() {
        Halstead halstead = methodHalstead("int m(int x) { return x++; }");

        assertEquals(2, halstead.uniqueOperators());
        assertEquals(2, halstead.totalOperators());
        assertEquals(2, halstead.uniqueOperands());
        assertEquals(3, halstead.totalOperands());
    }

    @Test
    void fileAndMethodValuesAreBothPresent() {
        FileMetrics file = metrics("""
                class A {
                    int m() { return 1; }
                }
                """);

        Halstead method = file.methods().getFirst().halstead();
        assertEquals(1, file.halstead().uniqueOperators());
        assertEquals(3, file.halstead().uniqueOperands());
        assertEquals(1, method.uniqueOperators());
        assertEquals(2, method.uniqueOperands());
        assertEquals(file.halstead().vocabulary(), file.halstead().uniqueOperators() + file.halstead().uniqueOperands());
        assertEquals(method.length(), method.totalOperators() + method.totalOperands());
    }

    private Halstead methodHalstead(String method) {
        FileMetrics file = metrics("class A { " + method + " }");
        assertEquals(1, file.methods().size());
        return file.methods().getFirst().halstead();
    }

    private FileMetrics metrics(String source) {
        AnalysisResult.Ok ok = assertInstanceOf(AnalysisResult.Ok.class, analyzer.analyze(source, "A.java"));
        return ok.file();
    }
}
