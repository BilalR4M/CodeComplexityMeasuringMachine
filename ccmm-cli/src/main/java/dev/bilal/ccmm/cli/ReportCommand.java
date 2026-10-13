package dev.bilal.ccmm.cli;

import dev.bilal.ccmm.core.AnalysisResult;
import dev.bilal.ccmm.core.Analyzer;
import dev.bilal.ccmm.core.FileMetrics;
import dev.bilal.ccmm.core.MethodMetrics;
import dev.bilal.ccmm.core.ProjectReport;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class ReportCommand {
    static final String USAGE = "usage: ccmm report <path> [--format markdown]";

    private ReportCommand() {}

    static int run(List<String> args, PrintStream out, PrintStream err) {
        String pathText = null;
        String format = "markdown";
        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            if ("--format".equals(arg)) {
                if (i + 1 >= args.size()) {
                    return usage(err);
                }
                format = args.get(++i);
            } else if (arg.startsWith("--format=")) {
                format = arg.substring("--format=".length());
            } else if (arg.startsWith("-")) {
                return usage(err);
            } else if (pathText == null) {
                pathText = arg;
            } else {
                return usage(err);
            }
        }
        if (pathText == null || !"markdown".equals(format)) {
            return usage(err);
        }
        Path path = Path.of(pathText);
        if (!Files.exists(path)) {
            err.println("path does not exist: " + pathText);
            return 2;
        }
        ProjectReport report = load(path, err);
        if (report == null) {
            return 2;
        }
        out.print(markdown(report));
        return 0;
    }

    private static ProjectReport load(Path path, PrintStream err) {
        try {
            if (Files.isDirectory(path)) {
                return new Analyzer().scan(path);
            }
            String relative = path.getFileName().toString();
            return new ProjectReport(List.of(new Analyzer().analyze(Files.readString(path), relative)), List.of());
        } catch (IOException | IllegalArgumentException exception) {
            err.println(exception.getMessage());
            return null;
        }
    }

    static String markdown(ProjectReport report) {
        StringBuilder out = new StringBuilder();
        out.append("# CCMM report\n");
        for (AnalysisResult result : report.files()) {
            switch (result) {
                case AnalysisResult.Ok ok -> appendOk(out, ok);
                case AnalysisResult.Failed failed -> {
                    out.append("\n## `").append(code(failed.relativePath())).append("`\n\n");
                    out.append("Parse failed: ").append(failed.message().replace("\r\n", "\n")).append('\n');
                }
            }
        }
        if (!report.skipped().isEmpty()) {
            out.append("\n## Skipped\n\n");
            for (String name : report.skipped()) {
                out.append("- `").append(code(name)).append("`\n");
            }
        }
        return out.toString();
    }

    private static void appendOk(StringBuilder out, AnalysisResult.Ok ok) {
        FileMetrics file = ok.file();
        out.append("\n## `").append(code(ok.relativePath())).append("`\n\n");
        out.append("- Maintainability: ").append(file.maintainability()).append('\n');
        out.append("- Cyclomatic: ").append(file.cyclomatic()).append('\n');
        out.append("- Nesting: ").append(file.maxLoopNesting()).append('\n');
        out.append("- Code lines: ").append(file.codeLines()).append('\n');
        out.append("- Volume: ").append(file.halstead().volume()).append('\n');
        out.append("\n| Method | Line | Cyclomatic | Nesting | Volume | Maintainability |\n");
        out.append("| --- | --- | --- | --- | --- | --- |\n");
        for (MethodMetrics method : file.methods()) {
            out.append("| ")
                    .append(cell(method.name())).append(" | ")
                    .append(method.startLine()).append(" | ")
                    .append(method.cyclomatic()).append(" | ")
                    .append(method.loopNesting()).append(" | ")
                    .append(method.halstead().volume()).append(" | ")
                    .append(method.maintainability()).append(" |\n");
        }
    }

    private static String cell(String value) {
        return value.replace("|", "\\|").replace("\n", " ");
    }

    private static String code(String value) {
        return value.replace("`", "");
    }

    private static int usage(PrintStream err) {
        err.println(USAGE);
        return 2;
    }
}
