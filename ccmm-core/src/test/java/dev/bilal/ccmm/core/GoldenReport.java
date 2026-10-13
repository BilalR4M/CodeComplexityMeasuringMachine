package dev.bilal.ccmm.core;

final class GoldenReport {
    private GoldenReport() {}

    static String format(FileMetrics file) {
        StringBuilder out = new StringBuilder();
        field(out, "file.physicalLines", file.physicalLines());
        field(out, "file.codeLines", file.codeLines());
        field(out, "file.commentLines", file.commentLines());
        field(out, "file.blankLines", file.blankLines());
        field(out, "file.cyclomatic", file.cyclomatic());
        field(out, "file.maxLoopNesting", file.maxLoopNesting());
        halstead(out, "file", file.halstead());
        field(out, "file.maintainability", file.maintainability());
        int index = 1;
        for (MethodMetrics method : file.methods()) {
            String prefix = "method." + index;
            field(out, prefix + ".name", method.name());
            field(out, prefix + ".startLine", method.startLine());
            field(out, prefix + ".cyclomatic", method.cyclomatic());
            field(out, prefix + ".loopNesting", method.loopNesting());
            halstead(out, prefix, method.halstead());
            field(out, prefix + ".maintainability", method.maintainability());
            index++;
        }
        return out.toString();
    }

    private static void halstead(StringBuilder out, String prefix, Halstead halstead) {
        field(out, prefix + ".uniqueOperators", halstead.uniqueOperators());
        field(out, prefix + ".uniqueOperands", halstead.uniqueOperands());
        field(out, prefix + ".totalOperators", halstead.totalOperators());
        field(out, prefix + ".totalOperands", halstead.totalOperands());
        field(out, prefix + ".vocabulary", halstead.vocabulary());
        field(out, prefix + ".length", halstead.length());
        field(out, prefix + ".volume", halstead.volume());
        field(out, prefix + ".difficulty", halstead.difficulty());
        field(out, prefix + ".effort", halstead.effort());
    }

    private static void field(StringBuilder out, String name, int value) {
        out.append(name).append('=').append(value).append('\n');
    }

    private static void field(StringBuilder out, String name, double value) {
        out.append(name).append('=').append(Double.toString(value)).append('\n');
    }

    private static void field(StringBuilder out, String name, String value) {
        out.append(name).append('=').append(value).append('\n');
    }
}
