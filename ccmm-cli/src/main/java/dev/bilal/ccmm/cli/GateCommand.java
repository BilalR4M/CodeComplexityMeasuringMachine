package dev.bilal.ccmm.cli;

import dev.bilal.ccmm.core.AnalysisResult;
import dev.bilal.ccmm.core.Analyzer;
import dev.bilal.ccmm.core.MethodMetrics;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class GateCommand {
    static final String USAGE = "usage: ccmm gate --base <git-ref> [--format markdown]";
    private static final long MAX_FILE_BYTES = 1024L * 1024L;

    private GateCommand() {}

    static int run(List<String> args, PrintStream out, PrintStream err, Path projectRoot) {
        String base = null;
        String format = "markdown";
        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            if ("--base".equals(arg)) {
                if (i + 1 >= args.size()) {
                    return usage(err);
                }
                base = args.get(++i);
            } else if (arg.startsWith("--base=")) {
                base = arg.substring("--base=".length());
            } else if ("--format".equals(arg)) {
                if (i + 1 >= args.size()) {
                    return usage(err);
                }
                format = args.get(++i);
            } else if (arg.startsWith("--format=")) {
                format = arg.substring("--format=".length());
            } else {
                return usage(err);
            }
        }
        if (base == null || base.isBlank() || !"markdown".equals(format)) {
            return usage(err);
        }
        GateRules rules;
        try {
            rules = GateRules.load(projectRoot);
        } catch (GateRules.GateRuleException exception) {
            err.println(exception.getMessage());
            out.print(summary(base, List.of(), List.of(), List.of(), exception.getMessage()));
            return 2;
        }
        Diff diff = changedJavaFiles(projectRoot, base);
        if (diff.error != null) {
            err.println(diff.error);
            out.print(summary(base, List.of(), List.of(), List.of(), diff.error));
            return 2;
        }
        List<Breach> breaches = new ArrayList<>();
        List<String> unparsed = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        Analyzer analyzer = new Analyzer();
        for (String relative : diff.files) {
            consider(projectRoot, relative, rules, analyzer, breaches, unparsed, skipped);
        }
        out.print(summary(base, breaches, unparsed, skipped, null));
        return breaches.isEmpty() ? 0 : 1;
    }

    private static void consider(
            Path root,
            String relative,
            GateRules rules,
            Analyzer analyzer,
            List<Breach> breaches,
            List<String> unparsed,
            List<String> skipped) {
        Path file = root.resolve(relative);
        try {
            if (Files.isSymbolicLink(file)) {
                Path real = file.toRealPath();
                if (!inside(root.toRealPath(), real)) {
                    skipped.add(relative);
                    return;
                }
            }
            if (!Files.isRegularFile(file)) {
                unparsed.add(relative + ": missing");
                return;
            }
            if (Files.size(file) > MAX_FILE_BYTES) {
                skipped.add(relative);
                return;
            }
            AnalysisResult result = analyzer.analyze(Files.readString(file), relative);
            switch (result) {
                case AnalysisResult.Ok ok -> {
                    for (MethodMetrics method : ok.file().methods()) {
                        if (rules.exceedsCyclomatic(method.cyclomatic())) {
                            breaches.add(new Breach(relative, method.name(), "cyclomatic",
                                    Integer.toString(method.cyclomatic()), Integer.toString(rules.cyclomatic())));
                        }
                        if (rules.exceedsNesting(method.loopNesting())) {
                            breaches.add(new Breach(relative, method.name(), "nesting",
                                    Integer.toString(method.loopNesting()), Integer.toString(rules.nesting())));
                        }
                        if (rules.belowMaintainability(method.maintainability())) {
                            breaches.add(new Breach(relative, method.name(), "maintainability",
                                    Double.toString(method.maintainability()), Double.toString(rules.maintainability())));
                        }
                    }
                }
                case AnalysisResult.Failed failed -> unparsed.add(relative + ": " + oneLine(failed.message()));
            }
        } catch (IOException exception) {
            unparsed.add(relative + ": " + oneLine(exception.getMessage()));
        }
    }

    private static Diff changedJavaFiles(Path root, String base) {
        Process process;
        try {
            process = new ProcessBuilder("git", "diff", "--name-only", "--diff-filter=ACMR", base + "...HEAD")
                    .directory(root.toFile())
                    .redirectErrorStream(true)
                    .start();
        } catch (IOException exception) {
            return new Diff(List.of(), "git diff failed: " + exception.getMessage());
        }
        String output;
        try {
            output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (process.waitFor() != 0) {
                String message = output.isBlank() ? "git diff failed" : output.strip();
                return new Diff(List.of(), message);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new Diff(List.of(), "git diff failed: " + exception.getMessage());
        } catch (IOException exception) {
            return new Diff(List.of(), "git diff failed: " + exception.getMessage());
        }
        List<String> files = new ArrayList<>();
        for (String line : output.split("\n", -1)) {
            String name = line.endsWith("\r") ? line.substring(0, line.length() - 1).trim() : line.trim();
            if (name.isEmpty() || !name.endsWith(".java") || !safeRelative(name)) {
                continue;
            }
            files.add(name.replace('\\', '/'));
        }
        return new Diff(files, null);
    }

    private static boolean safeRelative(String name) {
        Path path = Path.of(name);
        if (path.isAbsolute()) {
            return false;
        }
        for (Path part : path) {
            if ("..".equals(part.toString())) {
                return false;
            }
        }
        return true;
    }

    private static boolean inside(Path rootReal, Path real) {
        Path relative = rootReal.relativize(real.normalize());
        if (relative.isAbsolute()) {
            return false;
        }
        for (Path part : relative) {
            if ("..".equals(part.toString())) {
                return false;
            }
        }
        return true;
    }

    static String summary(String base, List<Breach> breaches, List<String> unparsed, List<String> skipped, String error) {
        StringBuilder out = new StringBuilder();
        out.append("# CCMM gate\n\n");
        out.append("Base: `").append(base.replace("`", "")).append("`\n\n");
        if (error != null) {
            out.append(error).append('\n');
            return out.toString();
        }
        if (breaches.isEmpty()) {
            out.append("No changed methods break the rules.\n");
        } else {
            out.append("| File | Method | Rule | Value | Limit |\n");
            out.append("| --- | --- | --- | --- | --- |\n");
            for (Breach breach : breaches) {
                out.append("| ")
                        .append(cell(breach.file)).append(" | ")
                        .append(cell(breach.method)).append(" | ")
                        .append(cell(breach.rule)).append(" | ")
                        .append(cell(breach.value)).append(" | ")
                        .append(cell(breach.limit)).append(" |\n");
            }
        }
        if (!unparsed.isEmpty()) {
            out.append("\n## Unparsed\n\n");
            for (String line : unparsed) {
                out.append("- ").append(line).append('\n');
            }
        }
        if (!skipped.isEmpty()) {
            out.append("\n## Skipped\n\n");
            for (String line : skipped) {
                out.append("- ").append(line).append('\n');
            }
        }
        return out.toString();
    }

    private static String cell(String value) {
        return value.replace("|", "\\|").replace("\n", " ");
    }

    private static String oneLine(String message) {
        return message == null ? "" : message.replace('\n', ' ').replace('\r', ' ');
    }

    private static int usage(PrintStream err) {
        err.println(USAGE);
        return 2;
    }

    private record Breach(String file, String method, String rule, String value, String limit) {}

    private record Diff(List<String> files, String error) {}
}
