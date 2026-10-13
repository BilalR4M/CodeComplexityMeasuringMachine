package dev.bilal.ccmm.core;

import java.util.List;

public record FileMetrics(
        int physicalLines,
        int codeLines,
        int commentLines,
        int blankLines,
        int cyclomatic,
        int maxLoopNesting,
        Halstead halstead,
        double maintainability,
        List<MethodMetrics> methods) {
    public FileMetrics {
        methods = List.copyOf(methods);
    }
}
