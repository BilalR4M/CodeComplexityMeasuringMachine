package dev.bilal.ccmm.core;

import com.github.javaparser.JavaToken;
import com.github.javaparser.JavaToken.Kind;
import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.CompilationUnit;
import java.util.EnumSet;
import java.util.Set;

final class LineCounter {
    record Counts(int physical, int code, int comment, int blank) {}

    private static final Set<Kind> COMMENTS = EnumSet.of(
            Kind.SINGLE_LINE_COMMENT,
            Kind.ENTER_JAVADOC_COMMENT,
            Kind.ENTER_MULTILINE_COMMENT,
            Kind.JAVADOC_COMMENT,
            Kind.MULTI_LINE_COMMENT,
            Kind.COMMENT_CONTENT);

    private static final Set<Kind> IGNORED = EnumSet.of(
            Kind.EOF, Kind.SPACE, Kind.WINDOWS_EOL, Kind.UNIX_EOL, Kind.OLD_MAC_EOL, Kind.CTRL_Z);

    private LineCounter() {}

    static Counts count(String source, CompilationUnit unit) {
        Classified lines = classify(source, unit);
        int physical = Math.max(0, lines.code.length - 1);
        int codeLines = 0;
        int commentLines = 0;
        int blankLines = 0;
        for (int line = 1; line <= physical; line++) {
            if (lines.code[line]) {
                codeLines++;
            } else if (lines.comment[line]) {
                commentLines++;
            } else {
                blankLines++;
            }
        }
        return new Counts(physical, codeLines, commentLines, blankLines);
    }

    /** Index 0 is unused. A true entry is a code line. */
    static boolean[] codeLines(String source, CompilationUnit unit) {
        return classify(source, unit).code;
    }

    static int codeLinesIn(boolean[] code, int beginLine, int endLine) {
        int from = Math.max(1, beginLine);
        int to = Math.min(code.length - 1, endLine);
        int count = 0;
        for (int line = from; line <= to; line++) {
            if (code[line]) {
                count++;
            }
        }
        return count;
    }

    private record Classified(boolean[] code, boolean[] comment) {}

    private static Classified classify(String source, CompilationUnit unit) {
        int physical = physicalLines(source);
        boolean[] code = new boolean[physical + 1];
        boolean[] comment = new boolean[physical + 1];
        if (physical > 0) {
            unit.getTokenRange().ifPresent(range -> mark(range, physical, code, comment));
        }
        return new Classified(code, comment);
    }

    private static void mark(TokenRange range, int physical, boolean[] code, boolean[] comment) {
        for (JavaToken token : range) {
            Kind kind = Kind.valueOf(token.getKind());
            if (IGNORED.contains(kind)) {
                continue;
            }
            token.getRange().ifPresent(span -> {
                int begin = Math.max(1, span.begin.line);
                int end = Math.min(physical, span.end.line);
                boolean[] target = COMMENTS.contains(kind) ? comment : code;
                for (int line = begin; line <= end; line++) {
                    target[line] = true;
                }
            });
        }
    }

    private static int physicalLines(String source) {
        if (source.isEmpty()) {
            return 0;
        }
        int lines = 1;
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '\n') {
                lines++;
            } else if (c == '\r') {
                lines++;
                if (i + 1 < source.length() && source.charAt(i + 1) == '\n') {
                    i++;
                }
            }
        }
        if (source.endsWith("\n") || source.endsWith("\r")) {
            lines--;
        }
        return lines;
    }
}
