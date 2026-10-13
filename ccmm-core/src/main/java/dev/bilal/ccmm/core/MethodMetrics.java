package dev.bilal.ccmm.core;

public record MethodMetrics(
        String name,
        int startLine,
        int codeLines,
        int cyclomatic,
        int loopNesting,
        Halstead halstead,
        double maintainability) {}
