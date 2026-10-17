package dev.bilal.ccmm.app.editor;

/** Token span. Token names are not colours (ADR-0009). */
public record HighlightSpan(int start, int end, String token) {
    public HighlightSpan {
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("bad range " + start + ".." + end);
        }
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("token required");
        }
    }
}
