package dev.bilal.ccmm.collab.protocol;

/**
 * A plain-text insert or delete based on a server revision.
 * Spike type only. Not the production protocol.
 */
record TextOp(Kind kind, int index, String text, int length, int baseRevision, String clientId) {
    enum Kind {
        INSERT,
        DELETE
    }

    static TextOp insert(int index, String text, int baseRevision, String clientId) {
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("insert text is empty");
        }
        return new TextOp(Kind.INSERT, index, text, text.length(), baseRevision, clientId);
    }

    static TextOp delete(int index, int length, int baseRevision, String clientId) {
        if (length < 0) {
            throw new IllegalArgumentException("delete length is negative");
        }
        return new TextOp(Kind.DELETE, index, "", length, baseRevision, clientId);
    }

    TextOp at(int newIndex) {
        return new TextOp(kind, newIndex, text, length, baseRevision, clientId);
    }

    TextOp withLength(int newLength) {
        return new TextOp(kind, index, text, newLength, baseRevision, clientId);
    }
}
