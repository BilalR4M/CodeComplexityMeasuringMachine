package dev.bilal.ccmm.app.editor;

/** Computes the indent inserted after a local Enter. Spaces only; width 4. */
public final class AutoIndent {
    public static final int WIDTH = 4;
    public static final String LEVEL = " ".repeat(WIDTH);

    private AutoIndent() {}

    /**
     * Text to insert for Enter: newline plus indent copied from {@code lineBeforeCaret},
     * plus one level when that line's trimmed form ends with {@code '{'}.
     */
    public static String insertion(String lineBeforeCaret) {
        return "\n" + indentAfter(lineBeforeCaret == null ? "" : lineBeforeCaret);
    }

    public static String indentAfter(String lineBeforeCaret) {
        String line = lineBeforeCaret == null ? "" : lineBeforeCaret;
        String leading = leadingWhitespace(line);
        if (line.strip().endsWith("{")) {
            return leading + LEVEL;
        }
        return leading;
    }

    static String leadingWhitespace(String line) {
        int i = 0;
        while (i < line.length()) {
            char ch = line.charAt(i);
            if (ch != ' ' && ch != '\t') {
                break;
            }
            i++;
        }
        return line.substring(0, i);
    }
}
