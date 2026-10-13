package dev.bilal.ccmm.core;

import com.github.javaparser.JavaParser;
import com.github.javaparser.Problem;
import com.github.javaparser.ast.CompilationUnit;
import java.nio.file.Path;
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

    public ProjectReport scan(Path projectRoot) {
        return ProjectScanner.scan(this, projectRoot);
    }

    private static FileMetrics fileMetrics(String source, CompilationUnit unit) {
        LineCounter.Counts lines = LineCounter.count(source, unit);
        boolean[] code = LineCounter.codeLines(source, unit);
        List<MethodMetrics> methods = Cyclomatic.methods(unit, code);
        int cyclomatic = methods.stream().mapToInt(MethodMetrics::cyclomatic).sum();
        int maxLoopNesting = methods.stream().mapToInt(MethodMetrics::loopNesting).max().orElse(0);
        Halstead halstead = HalsteadMetrics.measure(unit);
        double maintainability = Maintainability.index(halstead.volume(), cyclomatic, lines.code());
        return new FileMetrics(
                lines.physical(),
                lines.code(),
                lines.comment(),
                lines.blank(),
                cyclomatic,
                maxLoopNesting,
                halstead,
                maintainability,
                methods);
    }
}
