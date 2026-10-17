package dev.bilal.ccmm.app.editor;

import java.util.OptionalInt;

/** Parses and clamps a go-to-line request. Line numbers are 1-based. */
public final class GoToLine {
    public static final String PAST_END = "Line %d is past the end";

    public record Target(int line, boolean pastEnd, int requested) {
        public Target {
            if (line < 1) {
                throw new IllegalArgumentException("line must be >= 1");
            }
        }
    }

    private GoToLine() {}

    /** Digits only; non-digits are dropped. Empty or all non-digits → empty. */
    public static OptionalInt parse(String input) {
        if (input == null || input.isEmpty()) {
            return OptionalInt.empty();
        }
        StringBuilder digits = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (ch >= '0' && ch <= '9') {
                digits.append(ch);
            }
        }
        if (digits.isEmpty()) {
            return OptionalInt.empty();
        }
        try {
            int value = Integer.parseInt(digits.toString());
            if (value < 1) {
                return OptionalInt.empty();
            }
            return OptionalInt.of(value);
        } catch (NumberFormatException overflow) {
            return OptionalInt.empty();
        }
    }

    /** Clamps {@code requested} to {@code [1, lineCount]}. */
    public static Target clamp(int requested, int lineCount) {
        if (lineCount < 1) {
            throw new IllegalArgumentException("lineCount must be >= 1");
        }
        if (requested < 1) {
            throw new IllegalArgumentException("requested must be >= 1");
        }
        if (requested > lineCount) {
            return new Target(lineCount, true, requested);
        }
        return new Target(requested, false, requested);
    }

    public static String pastEndMessage(int requested) {
        return PAST_END.formatted(requested);
    }
}
