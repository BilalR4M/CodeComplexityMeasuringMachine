package dev.bilal.ccmm.cli;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        if (args.length == 0) {
            err.println(usage());
            return 2;
        }
        List<String> rest = AnalyzeCommand.argumentsAfterCommand(args);
        return switch (args[0]) {
            case "analyze" -> AnalyzeCommand.run(rest, out, err);
            case "gate" -> GateCommand.run(rest, out, err, Path.of("").toAbsolutePath());
            case "report" -> ReportCommand.run(rest, out, err);
            default -> {
                err.println(usage());
                yield 2;
            }
        };
    }

    private static String usage() {
        return AnalyzeCommand.USAGE + "\n" + GateCommand.USAGE + "\n" + ReportCommand.USAGE;
    }
}
