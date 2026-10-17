package dev.bilal.ccmm.app.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cancellable-friendly Java regex lexer (ADR-0009). Pure Java, no native code.
 * Token names: keyword, string, comment, number, identifier.
 */
public final class RegexJavaHighlighter {
    private static final Set<String> KEYWORDS = Set.of(
            "abstract",
            "assert",
            "boolean",
            "break",
            "byte",
            "case",
            "catch",
            "char",
            "class",
            "const",
            "continue",
            "default",
            "do",
            "double",
            "else",
            "enum",
            "extends",
            "final",
            "finally",
            "float",
            "for",
            "goto",
            "if",
            "implements",
            "import",
            "instanceof",
            "int",
            "interface",
            "long",
            "native",
            "new",
            "package",
            "private",
            "protected",
            "public",
            "return",
            "short",
            "static",
            "strictfp",
            "super",
            "switch",
            "synchronized",
            "this",
            "throw",
            "throws",
            "transient",
            "try",
            "void",
            "volatile",
            "while",
            "var",
            "record",
            "sealed",
            "permits",
            "yield",
            "true",
            "false",
            "null");

    private static final Pattern TOKEN = Pattern.compile(
            "(?s)"
                    + "(?<BLOCK>/\\*.*?\\*/)"
                    + "|(?<LINE>//[^\\n]*)"
                    + "|(?<STRING>\"(?:\\\\.|[^\"\\\\])*\")"
                    + "|(?<CHAR>'(?:\\\\.|[^'\\\\])*')"
                    + "|(?<NUMBER>\\b\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?[fFdDlL]?\\b)"
                    + "|(?<IDENT>\\b[A-Za-z_][A-Za-z0-9_]*\\b)");

    public List<HighlightSpan> highlight(String text, String relativePath) {
        if (text == null) {
            throw new IllegalArgumentException("text");
        }
        if (!isJavaPath(relativePath)) {
            return List.of();
        }
        List<HighlightSpan> spans = new ArrayList<>();
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
            spans.add(new HighlightSpan(matcher.start(), matcher.end(), token));
        }
        return spans;
    }

    private static boolean isJavaPath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return false;
        }
        String normalized = relativePath.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        String name = slash < 0 ? normalized : normalized.substring(slash + 1);
        return name.toLowerCase().endsWith(".java");
    }
}
