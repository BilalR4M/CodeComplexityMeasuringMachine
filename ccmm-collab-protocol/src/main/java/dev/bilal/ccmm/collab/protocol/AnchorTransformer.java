package dev.bilal.ccmm.collab.protocol;

import java.util.List;
import java.util.Optional;

/**
 * Transforms a {@link ThreadAnchor} through the same server-ordered ops as the text.
 * A range that collapses to empty is removed rather than kept as a zero-length span.
 */
public final class AnchorTransformer {
    private AnchorTransformer() {}

    /**
     * Returns the anchor after {@code op} is already in the document, or empty when
     * the range was fully deleted.
     */
    public static Optional<ThreadAnchor> next(int start, int end, TextOp op) {
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("bad range " + start + ".." + end);
        }
        if (op == null) {
            throw new IllegalArgumentException("op is null");
        }
        if (op.kind() == TextOp.Kind.INSERT) {
            return Optional.of(afterInsert(start, end, op.index(), op.text().length()));
        }
        return afterDelete(start, end, op.index(), op.length());
    }

    public static Optional<ThreadAnchor> next(ThreadAnchor anchor, TextOp op) {
        if (anchor == null) {
            throw new IllegalArgumentException("anchor is null");
        }
        return next(anchor.start(), anchor.end(), op);
    }

    public static Optional<ThreadAnchor> applyAll(ThreadAnchor anchor, List<TextOp> log) {
        Optional<ThreadAnchor> current = Optional.of(anchor);
        for (TextOp op : log) {
            if (current.isEmpty()) {
                return current;
            }
            current = next(current.get(), op);
        }
        return current;
    }

    private static ThreadAnchor afterInsert(int start, int end, int index, int length) {
        if (index <= start) {
            return new ThreadAnchor(start + length, end + length);
        }
        if (index < end) {
            return new ThreadAnchor(start, end + length);
        }
        return new ThreadAnchor(start, end);
    }

    private static Optional<ThreadAnchor> afterDelete(int start, int end, int deletedStart, int deletedLength) {
        if (deletedLength == 0) {
            return Optional.of(new ThreadAnchor(start, end));
        }
        int deletedEnd = deletedStart + deletedLength;
        int newStart = mapPoint(start, deletedStart, deletedEnd, deletedLength);
        int newEnd = mapPoint(end, deletedStart, deletedEnd, deletedLength);
        if (newEnd <= newStart) {
            return Optional.empty();
        }
        return Optional.of(new ThreadAnchor(newStart, newEnd));
    }

    private static int mapPoint(int point, int deletedStart, int deletedEnd, int deletedLength) {
        if (point <= deletedStart) {
            return point;
        }
        if (point >= deletedEnd) {
            return point - deletedLength;
        }
        return deletedStart;
    }
}
