package dev.bilal.ccmm.core;

import com.github.javaparser.JavaParser;
import com.github.javaparser.Problem;
import java.util.List;
import java.util.stream.Collectors;

public final class Analyzer {
    public AnalysisResult analyze(String source, String relativePath) {
        if (source == null) {
            return new AnalysisResult.Failed(relativePath, "source is null");
        }
        var parsed = new JavaParser().parse(source);
        if (parsed.isSuccessful() && parsed.getResult().isPresent()) {
            return new AnalysisResult.Ok(relativePath, unmeasured());
        }
        String message = parsed.getProblems().stream()
                .map(Problem::getMessage)
                .collect(Collectors.joining("\n"));
        if (message.isBlank()) {
            message = "parse failed";
        }
        return new AnalysisResult.Failed(relativePath, message);
    }

    /** Metric tasks fill these fields. A successful parse still returns {@code Ok}. */
    private static FileMetrics unmeasured() {
        Halstead halstead = new Halstead(0, 0, 0, 0, 0, 0, 0, 0, 0);
        return new FileMetrics(0, 0, 0, 0, 0, 0, halstead, 0, List.of());
    }
}
