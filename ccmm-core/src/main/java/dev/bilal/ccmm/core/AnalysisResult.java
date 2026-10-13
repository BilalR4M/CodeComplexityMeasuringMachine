package dev.bilal.ccmm.core;

public sealed interface AnalysisResult {
    record Ok(String relativePath, FileMetrics file) implements AnalysisResult {}

    record Failed(String relativePath, String message) implements AnalysisResult {}
}
