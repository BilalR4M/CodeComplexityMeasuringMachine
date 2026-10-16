package dev.bilal.ccmm.collab.protocol;

/**
 * A half-open range {@code [start, end)} in code units.
 * Used for comment threads; transformed with the same ops as the text.
 */
public record ThreadAnchor(int start, int end) {
    public ThreadAnchor {
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("bad range " + start + ".." + end);
        }
    }

    public boolean isEmpty() {
        return start == end;
    }
}
