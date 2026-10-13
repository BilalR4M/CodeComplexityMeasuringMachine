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
            return new AnalysisResult.Ok(relativePath, fileMetrics(source, unit));
        }
        String message = parsed.getProblems().stream()
                .map(Problem::getMessage)
                .collect(Collectors.joining("\n"));
        if (message.isBlank()) {
            message = "parse failed";
        }
        return new AnalysisResult.Failed(relativePath, message);
    }

    /** Later metric tasks fill nesting, Halstead, and maintainability. */
    private static FileMetrics fileMetrics(String source, CompilationUnit unit) {
        LineCounter.Counts lines = LineCounter.count(source, unit);
        List<MethodMetrics> methods = Cyclomatic.methods(unit);
        int cyclomatic = methods.stream().mapToInt(MethodMetrics::cyclomatic).sum();
        Halstead halstead = new Halstead(0, 0, 0, 0, 0, 0, 0, 0, 0);
        return new FileMetrics(
                lines.physical(),
                lines.code(),
                lines.comment(),
                lines.blank(),
                cyclomatic,
                0,
                halstead,
                0,
                methods);
    }
}
