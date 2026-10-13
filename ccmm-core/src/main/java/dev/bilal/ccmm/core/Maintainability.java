package dev.bilal.ccmm.core;

final class Maintainability {
    private Maintainability() {}

    static double index(double volume, int cyclomatic, int codeLines) {
        if (volume <= 0 || codeLines <= 0) {
            return 0;
        }
        double raw = (171 - 5.2 * Math.log(volume) - 0.23 * cyclomatic - 16.2 * Math.log(codeLines)) * 100 / 171;
        return Math.max(0, raw);
    }
}
