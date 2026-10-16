package dev.bilal.ccmm.spikes.e0t04;

import java.util.Locale;

/** WCAG relative-luminance contrast. Same formula named in TOKENS.md. */
final class Contrast {
    private Contrast() {}

    static double ratio(String foregroundHex, String backgroundHex) {
        double a = relativeLuminance(parse(foregroundHex));
        double b = relativeLuminance(parse(backgroundHex));
        double lighter = Math.max(a, b);
        double darker = Math.min(a, b);
        return (lighter + 0.05) / (darker + 0.05);
    }

    static boolean matches(double actual, double expected, double tolerance) {
        return Math.abs(actual - expected) <= tolerance;
    }

    static String format(double ratio) {
        return String.format(Locale.US, "%.2f:1", ratio);
    }

    private static int[] parse(String hex) {
        String value = hex.startsWith("#") ? hex.substring(1) : hex;
        if (value.length() != 6) {
            throw new IllegalArgumentException("hex " + hex);
        }
        return new int[] {
            Integer.parseInt(value.substring(0, 2), 16),
            Integer.parseInt(value.substring(2, 4), 16),
            Integer.parseInt(value.substring(4, 6), 16)
        };
    }

    private static double relativeLuminance(int[] rgb) {
        double r = channel(rgb[0] / 255.0);
        double g = channel(rgb[1] / 255.0);
        double b = channel(rgb[2] / 255.0);
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    private static double channel(double c) {
        return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }
}
