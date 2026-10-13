package dev.bilal.ccmm.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class LoopNestingTest {
    private final Analyzer analyzer = new Analyzer();

    @Test
    void nestedLoopsUseTheDeepestLevel() {
        assertEquals(1, depth("void m() { for (;;) {} }"));
        assertEquals(1, depth("void m(int[] a) { for (int x : a) {} }"));
        assertEquals(1, depth("void m() { for (;;) {} for (;;) {} }"));
        assertEquals(3, depth("""
                void m() {
                    for (;;) {
                        while (false) {
                            do { break; } while (false);
                        }
                    }
                }
                """));
    }

    @Test
    void identifiersThatLookLikeLoopsDoNotCount() {
        assertEquals(0, depth("""
                void doOnce() {
                    double value = 1.0;
                    String format = "x";
                }
                """));
    }

    @Test
    void legacySampleMultiplicationTableIsDepthTwo() throws Exception {
        Path cwd = Path.of("").toAbsolutePath();
        Path fixture = Files.exists(cwd.resolve("fixtures/legacy/sampleCode.txt"))
                ? cwd.resolve("fixtures/legacy/sampleCode.txt")
                : cwd.getParent().resolve("fixtures/legacy/sampleCode.txt");
        String source = Files.readString(fixture);
        FileMetrics file = metrics(source);
        MethodMetrics table = file.methods().stream()
                .filter(method -> method.name().equals("printMultiplicationTable"))
                .findFirst()
                .orElseThrow();

        assertEquals(2, table.loopNesting());
        assertEquals(2, file.maxLoopNesting());
    }

    private int depth(String method) {
        FileMetrics file = metrics("class A { " + method + " }");
        assertEquals(1, file.methods().size());
        assertEquals(file.methods().getFirst().loopNesting(), file.maxLoopNesting());
        return file.methods().getFirst().loopNesting();
    }

    private FileMetrics metrics(String source) {
        AnalysisResult.Ok ok = assertInstanceOf(AnalysisResult.Ok.class, analyzer.analyze(source, "A.java"));
        return ok.file();
    }
}
