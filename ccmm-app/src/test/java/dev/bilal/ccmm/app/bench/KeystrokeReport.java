package dev.bilal.ccmm.app.bench;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;

/** Pure helpers for the RichTextFX keystroke spike. No JavaFX types. */
final class KeystrokeReport {
    static final int LINES = 10_000;
    static final int SAMPLES = 200;
    static final double BUDGET_MS = 16.0;

    private KeystrokeReport() {}

    static String sample(int lines) {
        StringBuilder text = new StringBuilder(lines * 32);
        for (int i = 0; i < lines; i++) {
            if (i > 0) {
                text.append('\n');
            }
            text.append("    int value").append(i).append(" = ").append(i).append(" + 1;");
        }
        return text.toString();
    }

    static double percentileMillis(long[] nanos, double percentile) {
        if (nanos.length == 0) {
            throw new IllegalArgumentException("no samples");
        }
        long[] sorted = nanos.clone();
        Arrays.sort(sorted);
        int index = (int) Math.ceil(percentile * sorted.length) - 1;
        index = Math.max(0, Math.min(sorted.length - 1, index));
        return sorted[index] / 1_000_000.0;
    }

    static String format(String os, String command, double p95Ms, long memoryBytes) {
        String verdict = p95Ms < BUDGET_MS ? "go" : "no-go";
        String memoryMb = String.format(Locale.US, "%.1f", memoryBytes / (1024.0 * 1024.0));
        String p95 = String.format(Locale.US, "%.3f", p95Ms);
        StringBuilder report = new StringBuilder();
        report.append("# Keystroke latency\n\n");
        report.append("p95 is the time `CodeArea.insertText` blocked the JavaFX thread ");
        report.append("for one character, after a ").append(LINES).append("-line buffer was on screen. ");
        report.append("It does not include waiting for the next vsync.\n\n");
        report.append("- OS: ").append(os).append('\n');
        report.append("- Command: `").append(command).append("`\n");
        report.append("- Buffer: ").append(LINES).append(" lines\n");
        report.append("- Samples: ").append(SAMPLES).append(" single-character inserts\n");
        report.append("- p95: ").append(p95).append(" ms\n");
        report.append("- memory: ").append(memoryMb).append(" MB committed\n");
        if ("no-go".equals(verdict)) {
            report.append("\nP3-T02 is replaced with an editor change. ");
            report.append("Do not implement the RichTextFX editor on this result.\n");
        }
        report.append('\n').append(verdict).append('\n');
        return report.toString();
    }

    static String withEditorMeasurement(String existing, String os, String command, double p95Ms, long memoryBytes) {
        String section = editorSection(os, command, p95Ms, memoryBytes);
        int heading = existing.indexOf("\n## Editor\n");
        String base = heading < 0 ? existing.strip() : existing.substring(0, heading).strip();
        return base + "\n\n" + section;
    }

    private static String editorSection(String os, String command, double p95Ms, long memoryBytes) {
        String verdict = p95Ms < BUDGET_MS ? "go" : "no-go";
        String memoryMb = String.format(Locale.US, "%.1f", memoryBytes / (1024.0 * 1024.0));
        String p95 = String.format(Locale.US, "%.3f", p95Ms);
        StringBuilder report = new StringBuilder();
        report.append("## Editor\n\n");
        report.append("Fresh p95 for the editor `CodeArea`, after a ").append(LINES);
        report.append("-line buffer was on screen.\n\n");
        report.append("- OS: ").append(os).append('\n');
        report.append("- Command: `").append(command).append("`\n");
        report.append("- Buffer: ").append(LINES).append(" lines\n");
        report.append("- Samples: ").append(SAMPLES).append(" single-character inserts\n");
        report.append("- p95: ").append(p95).append(" ms\n");
        report.append("- memory: ").append(memoryMb).append(" MB committed\n");
        report.append('\n').append(verdict).append('\n');
        return report.toString();
    }

    static Path reportFile(Path cwd) {
        if (Files.exists(cwd.resolve("docs/benchmarks"))) {
            return cwd.resolve("docs/benchmarks/keystroke-latency.md");
        }
        return cwd.getParent().resolve("docs/benchmarks/keystroke-latency.md");
    }
}
