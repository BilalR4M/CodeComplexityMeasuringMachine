package dev.bilal.ccmm.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;

class CyclomaticTest {
    private final Analyzer analyzer = new Analyzer();

    @Test
    void straightLineMethodStartsAtOne() {
        assertEquals(1, methodComplexity("void m() { int x = 1; }"));
    }

    @Test
    void eachDecisionAddsOne() {
        assertEquals(2, methodComplexity("void m() { if (true) {} }"));
        assertEquals(2, methodComplexity("void m(int[] a) { for (int i = 0; i < a.length; i++) {} }"));
        assertEquals(2, methodComplexity("void m(int[] a) { for (int x : a) {} }"));
        assertEquals(2, methodComplexity("void m() { while (false) {} }"));
        assertEquals(2, methodComplexity("void m() { do {} while (false); }"));
        assertEquals(2, methodComplexity("void m(int x) { switch (x) { case 1: break; } }"));
        assertEquals(2, methodComplexity("void m() { try {} catch (IllegalStateException e) {} }"));
        assertEquals(2, methodComplexity("boolean m(boolean a, boolean b) { return a && b; }"));
        assertEquals(2, methodComplexity("boolean m(boolean a, boolean b) { return a || b; }"));
        assertEquals(2, methodComplexity("int m(boolean a) { return a ? 1 : 0; }"));
    }

    @Test
    void elseReturnThrowAndSwitchDoNotAdd() {
        assertEquals(2, methodComplexity("""
                void m(boolean a) {
                    if (a) {
                        return;
                    } else {
                        throw new IllegalStateException();
                    }
                }
                """));
        assertEquals(3, methodComplexity("void m(boolean a, boolean b) { if (a) {} else if (b) {} }"));
        assertEquals(2, methodComplexity("""
                void m(int x) {
                    switch (x) {
                        case 1: return;
                        default: throw new IllegalStateException();
                    }
                }
                """));
        assertEquals(1, methodComplexity("int m(int a, int b) { return a & b | 1; }"));
    }

    @Test
    void fileValueIsTheSumOfItsMethods() {
        FileMetrics file = metrics("""
                class A {
                    void plain() {}
                    void branched() { if (true) {} }
                    A() { while (false) {} }
                }
                """);

        assertEquals(3, file.methods().size());
        assertEquals(5, file.cyclomatic());
    }

    private int methodComplexity(String method) {
        FileMetrics file = metrics("class A { " + method + " }");
        assertEquals(1, file.methods().size());
        return file.methods().getFirst().cyclomatic();
    }

    private FileMetrics metrics(String source) {
        AnalysisResult.Ok ok = assertInstanceOf(AnalysisResult.Ok.class, analyzer.analyze(source, "A.java"));
        int sum = ok.file().methods().stream().mapToInt(MethodMetrics::cyclomatic).sum();
        assertEquals(sum, ok.file().cyclomatic());
        return ok.file();
    }
}
