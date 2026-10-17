package dev.bilal.ccmm.app.editor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/**
 * Finds the matching (), [], or {} for the caret, skipping strings and comments.
 * Scans at most {@code window} characters on each side of the caret.
 */
public final class BracketMatcher {
    public static final int DEFAULT_WINDOW = 5_000;

    public record Match(int open, int close) {
        public Match {
            if (open < 0 || close <= open) {
                throw new IllegalArgumentException("bad match [" + open + ", " + close + "]");
            }
        }
    }

    private enum State {
        CODE,
        LINE_COMMENT,
        BLOCK_COMMENT,
        STRING,
        CHAR
    }

    private BracketMatcher() {}

    public static Optional<Match> find(String text, int caret) {
        return find(text, caret, DEFAULT_WINDOW);
    }

    public static Optional<Match> find(String text, int caret, int window) {
        if (text == null || text.isEmpty() || window < 1) {
            return Optional.empty();
        }
        int len = text.length();
        if (caret < 0 || caret > len) {
            return Optional.empty();
        }
        int from = Math.max(0, caret - window);
        int to = Math.min(len, caret + window);
        int bracket = bracketAtCaret(text, caret, from, to);
        if (bracket < 0 || inStringOrComment(text, from, bracket)) {
            return Optional.empty();
        }
        char ch = text.charAt(bracket);
        if (isOpen(ch)) {
            return matchForward(text, bracket, to);
        }
        if (isClose(ch)) {
            return matchBackward(text, bracket, from);
        }
        return Optional.empty();
    }

    /** Index of the bracket under or immediately left of the caret, else -1. */
    static int bracketAtCaret(String text, int caret, int from, int to) {
        if (caret > from && caret <= text.length()) {
            char left = text.charAt(caret - 1);
            if (isBracket(left)) {
                return caret - 1;
            }
        }
        if (caret >= from && caret < to && caret < text.length()) {
            char right = text.charAt(caret);
            if (isBracket(right)) {
                return caret;
            }
        }
        return -1;
    }

    private static Optional<Match> matchForward(String text, int openAt, int to) {
        char close = closer(text.charAt(openAt));
        Deque<Character> stack = new ArrayDeque<>();
        stack.push(text.charAt(openAt));
        State state = State.CODE;
        for (int i = openAt + 1; i < to; i++) {
            char ch = text.charAt(i);
            char prev = text.charAt(i - 1);
            if (state == State.CODE) {
                if (isOpen(ch)) {
                    stack.push(ch);
                } else if (isClose(ch)) {
                    if (stack.isEmpty()) {
                        return Optional.empty();
                    }
                    char expected = closer(stack.pop());
                    if (ch != expected) {
                        return Optional.empty();
                    }
                    if (stack.isEmpty()) {
                        return ch == close ? Optional.of(new Match(openAt, i)) : Optional.empty();
                    }
                }
            }
            state = step(state, ch, prev, i, text);
        }
        return Optional.empty();
    }

    private static Optional<Match> matchBackward(String text, int closeAt, int from) {
        char open = opener(text.charAt(closeAt));
        boolean[] code = codeMask(text, from, closeAt + 1);
        Deque<Character> stack = new ArrayDeque<>();
        stack.push(text.charAt(closeAt));
        for (int i = closeAt - 1; i >= from; i--) {
            if (!code[i - from]) {
                continue;
            }
            char ch = text.charAt(i);
            if (isClose(ch)) {
                stack.push(ch);
            } else if (isOpen(ch)) {
                if (stack.isEmpty()) {
                    return Optional.empty();
                }
                char expected = opener(stack.pop());
                if (ch != expected) {
                    return Optional.empty();
                }
                if (stack.isEmpty()) {
                    return ch == open ? Optional.of(new Match(i, closeAt)) : Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    /** True when {@code index} lies inside a string or comment, scanning from {@code from}. */
    static boolean inStringOrComment(String text, int from, int index) {
        if (index < from || index >= text.length()) {
            return false;
        }
        State state = State.CODE;
        for (int i = from; i < index; i++) {
            char ch = text.charAt(i);
            char prev = i > 0 ? text.charAt(i - 1) : '\0';
            state = step(state, ch, prev, i, text);
        }
        return state != State.CODE;
    }

    /** {@code true} at indices that are code (not string/comment content or delimiters). */
    private static boolean[] codeMask(String text, int from, int end) {
        boolean[] code = new boolean[end - from];
        State state = State.CODE;
        for (int i = from; i < end; i++) {
            code[i - from] = state == State.CODE;
            char ch = text.charAt(i);
            char prev = i > 0 ? text.charAt(i - 1) : '\0';
            State next = step(state, ch, prev, i, text);
            if (state == State.CODE && next != State.CODE) {
                code[i - from] = false; // opening delimiter
            }
            state = next;
        }
        return code;
    }

    private static State step(State state, char ch, char prev, int index, String text) {
        return switch (state) {
            case CODE -> {
                if (ch == '/' && index + 1 < text.length() && text.charAt(index + 1) == '/') {
                    yield State.LINE_COMMENT;
                }
                if (ch == '/' && index + 1 < text.length() && text.charAt(index + 1) == '*') {
                    yield State.BLOCK_COMMENT;
                }
                if (ch == '"') {
                    yield State.STRING;
                }
                if (ch == '\'') {
                    yield State.CHAR;
                }
                yield State.CODE;
            }
            case LINE_COMMENT -> ch == '\n' ? State.CODE : State.LINE_COMMENT;
            case BLOCK_COMMENT -> prev == '*' && ch == '/' ? State.CODE : State.BLOCK_COMMENT;
            case STRING -> ch == '"' && !oddEscapes(text, index) ? State.CODE : State.STRING;
            case CHAR -> ch == '\'' && !oddEscapes(text, index) ? State.CODE : State.CHAR;
        };
    }

    private static boolean oddEscapes(String text, int quoteIndex) {
        int escapes = 0;
        for (int i = quoteIndex - 1; i >= 0 && text.charAt(i) == '\\'; i--) {
            escapes++;
        }
        return escapes % 2 == 1;
    }

    private static boolean isBracket(char ch) {
        return isOpen(ch) || isClose(ch);
    }

    private static boolean isOpen(char ch) {
        return ch == '(' || ch == '[' || ch == '{';
    }

    private static boolean isClose(char ch) {
        return ch == ')' || ch == ']' || ch == '}';
    }

    private static char closer(char open) {
        return switch (open) {
            case '(' -> ')';
            case '[' -> ']';
            case '{' -> '}';
            default -> throw new IllegalArgumentException("not an open bracket: " + open);
        };
    }

    private static char opener(char close) {
        return switch (close) {
            case ')' -> '(';
            case ']' -> '[';
            case '}' -> '{';
            default -> throw new IllegalArgumentException("not a close bracket: " + close);
        };
    }
}
