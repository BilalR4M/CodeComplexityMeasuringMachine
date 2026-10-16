package dev.bilal.ccmm.spikes.e0t04;

import java.util.ArrayList;
import java.util.List;

/** Verifies primary and secondary text ratios against TOKENS.md. */
final class TokenCheck {
    private TokenCheck() {}

    record Result(boolean ok, List<String> lines) {}

    static Result verify() {
        List<String> lines = new ArrayList<>();
        boolean ok = true;
        ok &= check(lines, "dark text-primary", "#E8E8E8", "#1E1E1E", 13.61);
        ok &= check(lines, "dark text-secondary", "#B0B0B0", "#1E1E1E", 7.69);
        ok &= check(lines, "light text-primary", "#1E1E1E", "#FFFFFF", 16.67);
        ok &= check(lines, "light text-secondary", "#4A4A4A", "#FFFFFF", 8.86);
        return new Result(ok, lines);
    }

    private static boolean check(List<String> lines, String name, String fg, String bg, double expected) {
        double actual = Contrast.ratio(fg, bg);
        boolean match = Contrast.matches(actual, expected, 0.05);
        lines.add(name + ": " + Contrast.format(actual) + " (TOKENS.md " + Contrast.format(expected) + ") "
                + (match ? "ok" : "FAIL"));
        return match && actual >= 4.5;
    }
}
