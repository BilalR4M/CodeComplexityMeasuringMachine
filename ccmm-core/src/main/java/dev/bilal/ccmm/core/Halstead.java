package dev.bilal.ccmm.core;

public record Halstead(
        int uniqueOperators,
        int uniqueOperands,
        int totalOperators,
        int totalOperands,
        int vocabulary,
        int length,
        double volume,
        double difficulty,
        double effort) {}
