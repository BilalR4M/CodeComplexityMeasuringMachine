package dev.bilal.ccmm.app.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Finds non-overlapping literal matches. No regex. */
public final class BufferFinder {
    public record Match(int start, int end) {
        public Match {
            if (start < 0 || end < start) {
                throw new IllegalArgumentException("bad match [" + start + ", " + end + ")");
            }
        }

        public int length() {
            return end - start;
        }
    }

    private BufferFinder() {}

    public static List<Match> findAll(String text, String query, boolean matchCase) {
        Objects.requireNonNull(text);
        if (query == null || query.isEmpty()) {
            return List.of();
        }
        String haystack = matchCase ? text : text.toLowerCase(Locale.ROOT);
        String needle = matchCase ? query : query.toLowerCase(Locale.ROOT);
        List<Match> matches = new ArrayList<>();
        int from = 0;
        while (from <= haystack.length() - needle.length()) {
            int at = haystack.indexOf(needle, from);
            if (at < 0) {
                break;
            }
            matches.add(new Match(at, at + needle.length()));
            from = at + needle.length();
        }
        return List.copyOf(matches);
    }

    /** Next match at or after {@code caret}, wrapping to the first match. */
    public static int nextIndex(List<Match> matches, int caret) {
        if (matches.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < matches.size(); i++) {
            if (matches.get(i).start() >= caret) {
                return i;
            }
        }
        return 0;
    }

    /** Previous match strictly before {@code caret}, wrapping to the last match. */
    public static int previousIndex(List<Match> matches, int caret) {
        if (matches.isEmpty()) {
            return -1;
        }
        for (int i = matches.size() - 1; i >= 0; i--) {
            if (matches.get(i).start() < caret) {
                return i;
            }
        }
        return matches.size() - 1;
    }

    public static String replaceAll(String text, String query, String replacement, boolean matchCase) {
        Objects.requireNonNull(text);
        if (query == null || query.isEmpty()) {
            return text;
        }
        String insert = replacement == null ? "" : replacement;
        List<Match> matches = findAll(text, query, matchCase);
        if (matches.isEmpty()) {
            return text;
        }
        StringBuilder out = new StringBuilder(text.length());
        int cursor = 0;
        for (Match match : matches) {
            out.append(text, cursor, match.start());
            out.append(insert);
            cursor = match.end();
        }
        out.append(text, cursor, text.length());
        return out.toString();
    }
}
