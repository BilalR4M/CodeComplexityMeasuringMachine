package dev.bilal.ccmm.spikes.e0t02;

/** Token span. Token names are not colours. */
public record Span(int start, int end, String token) {
    public Span {
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("bad range " + start + ".." + end);
        }
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("token required");
        }
    }
}
