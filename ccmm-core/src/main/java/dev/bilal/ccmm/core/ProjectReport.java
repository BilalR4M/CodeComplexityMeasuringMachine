package dev.bilal.ccmm.core;

import java.util.List;

public record ProjectReport(List<AnalysisResult> files, List<String> skipped) {
    public ProjectReport {
        files = List.copyOf(files);
        skipped = List.copyOf(skipped);
    }
}
