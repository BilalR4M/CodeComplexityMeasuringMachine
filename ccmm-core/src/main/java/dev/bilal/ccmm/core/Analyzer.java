package dev.bilal.ccmm.core;

import com.github.javaparser.JavaParser;
import com.github.javaparser.Problem;
import com.github.javaparser.ast.CompilationUnit;
import java.util.List;
import java.util.stream.Collectors;

public final class Analyzer {
    public AnalysisResult analyze(String source, String relativePath) {
        if (source == null) {
            return new AnalysisResult.Failed(relativePath, "source is null");
        }
        var parsed = new JavaParser().parse(source);
        if (parsed.isSuccessful() && parsed.getResult().isPresent()) {
            CompilationUnit unit = parsed.getResult().get();
            return new AnalysisResult.Ok(relativePath, withLineCounts(source, unit));
        }
        String message = parsed.getProblems().stream()
                .map(Problem::getMessage)
                .collect(Collectors.joining("\n"));
        if (message.isBlank()) {
            message = "parse failed";
        }
        return new AnalysisResult.Failed(relativePath, message);
    }

    /** Later metric tasks fill the fields after the line counts. */
    private static FileMetrics withLineCounts(String source, CompilationUnit unit) {
        LineCounter.Counts lines = LineCounter.count(source, unit);
        Halstead halstead = new Halstead(0, 0, 0, 0, 0, 0, 0, 0, 0);
        return new FileMetrics(
                lines.physical(),
                lines.code(),
                lines.comment(),
                lines.blank(),
                0,
                0,
                halstead,
                0,
                List.of());
    }
}
