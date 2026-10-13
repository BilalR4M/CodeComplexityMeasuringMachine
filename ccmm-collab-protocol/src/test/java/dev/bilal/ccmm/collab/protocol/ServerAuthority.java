package dev.bilal.ccmm.collab.protocol;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-authoritative operational transform for plain text.
 * The server orders operations. A later operation is shifted so it still
 * means the same edit after earlier operations have been applied.
 */
final class ServerAuthority {
    private final String base;
    private final StringBuilder document;
    private final List<TextOp> log = new ArrayList<>();

    ServerAuthority(String base) {
        this.base = base;
        this.document = new StringBuilder(base);
    }

    TextOp receive(TextOp incoming) {
        TextOp transformed = incoming;
        for (int revision = incoming.baseRevision(); revision < log.size(); revision++) {
            transformed = transform(transformed, log.get(revision));
        }
        apply(document, transformed);
        log.add(transformed);
        return transformed;
    }

    String document() {
        return document.toString();
    }

    List<TextOp> log() {
        return List.copyOf(log);
    }

    static String replay(String base, List<TextOp> log) {
        StringBuilder text = new StringBuilder(base);
        for (TextOp op : log) {
            apply(text, op);
        }
        return text.toString();
    }

    String base() {
        return base;
    }

    static void apply(StringBuilder text, TextOp op) {
        if (op.kind() == TextOp.Kind.INSERT) {
            text.insert(op.index(), op.text());
            return;
        }
        if (op.length() == 0) {
            return;
        }
        text.delete(op.index(), op.index() + op.length());
    }

    /**
     * Returns {@code incoming} as it should be applied after {@code applied}
     * is already in the document.
     */
    static TextOp transform(TextOp incoming, TextOp applied) {
        if (incoming.kind() == TextOp.Kind.INSERT && applied.kind() == TextOp.Kind.INSERT) {
            if (applied.index() <= incoming.index()) {
                return incoming.at(incoming.index() + applied.text().length());
            }
            return incoming;
        }
        if (incoming.kind() == TextOp.Kind.INSERT && applied.kind() == TextOp.Kind.DELETE) {
            int start = applied.index();
            int end = applied.index() + applied.length();
            if (incoming.index() <= start) {
                return incoming;
            }
            if (incoming.index() >= end) {
                return incoming.at(incoming.index() - applied.length());
            }
            return incoming.at(start);
        }
        if (incoming.kind() == TextOp.Kind.DELETE && applied.kind() == TextOp.Kind.INSERT) {
            int start = incoming.index();
            int end = incoming.index() + incoming.length();
            int insertAt = applied.index();
            if (insertAt <= start) {
                return incoming.at(start + applied.text().length());
            }
            if (insertAt >= end) {
                return incoming;
            }
            return incoming.withLength(incoming.length() + applied.text().length());
        }
        return transformDelete(incoming, applied);
    }

    private static TextOp transformDelete(TextOp incoming, TextOp applied) {
        int appliedStart = applied.index();
        int appliedEnd = applied.index() + applied.length();
        int start = incoming.index();
        int end = incoming.index() + incoming.length();
        int newStart = mapPoint(start, appliedStart, appliedEnd, applied.length());
        int newEnd = mapPoint(end, appliedStart, appliedEnd, applied.length());
        if (newEnd < newStart) {
            newEnd = newStart;
        }
        return incoming.at(newStart).withLength(newEnd - newStart);
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
