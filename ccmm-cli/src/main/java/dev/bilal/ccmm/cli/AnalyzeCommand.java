package dev.bilal.ccmm.cli;

import dev.bilal.ccmm.core.AnalysisResult;
import dev.bilal.ccmm.core.Analyzer;
import dev.bilal.ccmm.core.FileMetrics;
import dev.bilal.ccmm.core.Halstead;
import dev.bilal.ccmm.core.MethodMetrics;
import dev.bilal.ccmm.core.ProjectReport;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class AnalyzeCommand {
    static final String USAGE = "usage: ccmm analyze <path> [--format text|json]";

    private AnalyzeCommand() {}

    static int run(List<String> args, PrintStream out, PrintStream err) {
        String pathText = null;
        String format = "text";
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
        if (pathText == null || (!"text".equals(format) && !"json".equals(format))) {
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
        out.print("json".equals(format) ? json(report) : text(report));
        return 0;
    }

    private static ProjectReport load(Path path, PrintStream err) {
        try {
            if (Files.isDirectory(path)) {
                return new Analyzer().scan(path);
            }
            String relative = path.getFileName().toString();
            AnalysisResult result = new Analyzer().analyze(Files.readString(path), relative);
            return new ProjectReport(List.of(result), List.of());
        } catch (IOException | IllegalArgumentException exception) {
            err.println(exception.getMessage());
            return null;
        }
    }

    private static int usage(PrintStream err) {
        err.println(USAGE);
        return 2;
    }

    static String text(ProjectReport report) {
        StringBuilder out = new StringBuilder();
        for (AnalysisResult result : report.files()) {
            switch (result) {
                case AnalysisResult.Ok ok -> appendOk(out, ok);
                case AnalysisResult.Failed failed -> {
                    out.append(failed.relativePath()).append('\n');
                    out.append("  failed\n");
                    out.append(failed.message()).append('\n');
                }
            }
        }
        if (!report.skipped().isEmpty()) {
            out.append("skipped\n");
            for (String name : report.skipped()) {
                out.append("  ").append(name).append('\n');
            }
        }
        return out.toString();
    }

    private static void appendOk(StringBuilder out, AnalysisResult.Ok ok) {
        FileMetrics file = ok.file();
        out.append(ok.relativePath()).append('\n');
        out.append("  ok")
                .append(" cyclomatic=").append(file.cyclomatic())
                .append(" nesting=").append(file.maxLoopNesting())
                .append(" code=").append(file.codeLines())
                .append(" maintainability=").append(file.maintainability())
                .append('\n');
        for (MethodMetrics method : file.methods()) {
            out.append("  ").append(method.name())
                    .append(" line=").append(method.startLine())
                    .append(" cyclomatic=").append(method.cyclomatic())
                    .append(" nesting=").append(method.loopNesting())
                    .append(" maintainability=").append(method.maintainability())
                    .append('\n');
        }
    }

    static String json(ProjectReport report) {
        StringBuilder out = new StringBuilder();
        out.append("{\n  \"files\": [\n");
        for (int i = 0; i < report.files().size(); i++) {
            writeResult(out, report.files().get(i));
            if (i + 1 < report.files().size()) {
                out.append(',');
            }
            out.append('\n');
        }
        out.append("  ],\n  \"skipped\": [");
        for (int i = 0; i < report.skipped().size(); i++) {
            if (i > 0) {
                out.append(", ");
            }
            string(out, report.skipped().get(i));
        }
        out.append("]\n}\n");
        return out.toString();
    }

    private static void writeResult(StringBuilder out, AnalysisResult result) {
        out.append("    { ");
        switch (result) {
            case AnalysisResult.Ok ok -> {
                field(out, "kind", "ok");
                out.append(", ");
                field(out, "relativePath", ok.relativePath());
                out.append(", \"file\": ");
                writeFile(out, ok.file());
            }
            case AnalysisResult.Failed failed -> {
                field(out, "kind", "failed");
                out.append(", ");
                field(out, "relativePath", failed.relativePath());
                out.append(", ");
                field(out, "message", failed.message());
            }
        }
        out.append(" }");
    }

    private static void writeFile(StringBuilder out, FileMetrics file) {
        out.append("{ ");
        out.append("\"physicalLines\": ").append(file.physicalLines());
        out.append(", \"codeLines\": ").append(file.codeLines());
        out.append(", \"commentLines\": ").append(file.commentLines());
        out.append(", \"blankLines\": ").append(file.blankLines());
        out.append(", \"cyclomatic\": ").append(file.cyclomatic());
        out.append(", \"maxLoopNesting\": ").append(file.maxLoopNesting());
        out.append(", \"halstead\": ");
        writeHalstead(out, file.halstead());
        out.append(", \"maintainability\": ").append(file.maintainability());
        out.append(", \"methods\": [");
        List<MethodMetrics> methods = file.methods();
        for (int i = 0; i < methods.size(); i++) {
            if (i > 0) {
                out.append(", ");
            }
            writeMethod(out, methods.get(i));
        }
        out.append("] }");
    }

    private static void writeMethod(StringBuilder out, MethodMetrics method) {
        out.append("{ ");
        field(out, "name", method.name());
        out.append(", \"startLine\": ").append(method.startLine());
        out.append(", \"cyclomatic\": ").append(method.cyclomatic());
        out.append(", \"loopNesting\": ").append(method.loopNesting());
        out.append(", \"halstead\": ");
        writeHalstead(out, method.halstead());
        out.append(", \"maintainability\": ").append(method.maintainability());
        out.append(" }");
    }

    private static void writeHalstead(StringBuilder out, Halstead halstead) {
        out.append("{ ");
        out.append("\"uniqueOperators\": ").append(halstead.uniqueOperators());
        out.append(", \"uniqueOperands\": ").append(halstead.uniqueOperands());
        out.append(", \"totalOperators\": ").append(halstead.totalOperators());
        out.append(", \"totalOperands\": ").append(halstead.totalOperands());
        out.append(", \"vocabulary\": ").append(halstead.vocabulary());
        out.append(", \"length\": ").append(halstead.length());
        out.append(", \"volume\": ").append(halstead.volume());
        out.append(", \"difficulty\": ").append(halstead.difficulty());
        out.append(", \"effort\": ").append(halstead.effort());
        out.append(" }");
    }

    private static void field(StringBuilder out, String name, String value) {
        string(out, name);
        out.append(": ");
        string(out, value);
    }

    private static void string(StringBuilder out, String value) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }

    static List<String> argumentsAfterCommand(String[] args) {
        List<String> rest = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            rest.add(args[i]);
        }
        return rest;
    }
}
