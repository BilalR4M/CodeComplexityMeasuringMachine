package dev.bilal.ccmm.spikes.e0t02;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cancellable-friendly Java regex lexer. Pure Java, no native code.
 * Covers keywords, strings, comments, numbers, and identifiers well enough for a bake-off.
 */
public final class RegexJavaHighlighter implements Highlighter {
    private static final Set<String> KEYWORDS = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
            "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
            "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
            "interface", "long", "native", "new", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
            "throw", "throws", "transient", "try", "void", "volatile", "while", "var", "record",
            "sealed", "permits", "yield", "true", "false", "null");

    private static final Pattern TOKEN = Pattern.compile(
            "(?s)"
                    + "(?<BLOCK>/\\*.*?\\*/)"
                    + "|(?<LINE>//[^\\n]*)"
                    + "|(?<STRING>\"(?:\\\\.|[^\"\\\\])*\")"
                    + "|(?<CHAR>'(?:\\\\.|[^'\\\\])*')"
                    + "|(?<NUMBER>\\b\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?[fFdDlL]?\\b)"
                    + "|(?<IDENT>\\b[A-Za-z_][A-Za-z0-9_]*\\b)");

    @Override
    public List<Span> highlight(String text, String extension) {
        if (text == null) {
            throw new IllegalArgumentException("text");
        }
        if (extension != null
                && !extension.isBlank()
                && !extension.equalsIgnoreCase("java")
                && !extension.equalsIgnoreCase(".java")) {
            return List.of();
        }
        List<Span> spans = new ArrayList<>();
        Matcher matcher = TOKEN.matcher(text);
        while (matcher.find()) {
            String token;
            if (matcher.group("BLOCK") != null || matcher.group("LINE") != null) {
                token = "comment";
            } else if (matcher.group("STRING") != null || matcher.group("CHAR") != null) {
                token = "string";
            } else if (matcher.group("NUMBER") != null) {
                token = "number";
            } else {
                String ident = matcher.group("IDENT");
                token = KEYWORDS.contains(ident) ? "keyword" : "identifier";
            }
            spans.add(new Span(matcher.start(), matcher.end(), token));
        }
        return spans;
    }
}
