package dev.bilal.ccmm.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.github.javaparser.JavaParser;
import com.github.javaparser.Problem;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class AnalyzerTest {
    private final Analyzer analyzer = new Analyzer();

    @Test
    void validCompilationUnitIsOk() {
        String source = """
                package sample;

                public class Two {
                    int value;

                    int doubled() {
                        return value * 2;
                    }
                }
                """;

        AnalysisResult result = analyzer.analyze(source, "src/Two.java");

        AnalysisResult.Ok ok = assertInstanceOf(AnalysisResult.Ok.class, result);
        assertEquals("src/Two.java", ok.relativePath());
    }

    @Test
    void syntaxErrorIsFailedWithTheParserMessage() {
        String source = "class {";
        String parserMessage = new JavaParser().parse(source).getProblems().stream()
                .map(Problem::getMessage)
                .collect(Collectors.joining("\n"));
        assertFalse(parserMessage.isBlank());

        AnalysisResult result = analyzer.analyze(source, "src/Broken.java");

        AnalysisResult.Failed failed = assertInstanceOf(AnalysisResult.Failed.class, result);
        assertEquals("src/Broken.java", failed.relativePath());
        assertEquals(parserMessage, failed.message());
    }

    @Test
    void nullSourceIsFailed() {
        AnalysisResult result = analyzer.analyze(null, "src/Missing.java");

        AnalysisResult.Failed failed = assertInstanceOf(AnalysisResult.Failed.class, result);
        assertEquals("src/Missing.java", failed.relativePath());
        assertFalse(failed.message().isBlank());
    }
}
