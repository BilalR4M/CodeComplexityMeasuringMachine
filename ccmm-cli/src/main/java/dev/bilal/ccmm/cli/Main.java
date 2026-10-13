package dev.bilal.ccmm.cli;

import java.io.PrintStream;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        if (args.length == 0) {
            err.println(AnalyzeCommand.USAGE);
            return 2;
        }
        if (!"analyze".equals(args[0])) {
            err.println(AnalyzeCommand.USAGE);
            return 2;
        }
        return AnalyzeCommand.run(AnalyzeCommand.argumentsAfterCommand(args), out, err);
    }
}
