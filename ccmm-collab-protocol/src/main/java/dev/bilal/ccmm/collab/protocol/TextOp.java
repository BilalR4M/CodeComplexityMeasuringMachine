package dev.bilal.ccmm.collab.protocol;

/**
 * A plain-text insert or delete based on a server revision.
 * The index is a code-unit offset.
 */
public record TextOp(Kind kind, int index, String text, int length, int baseRevision, String clientId) {
    public enum Kind {
        INSERT,
        DELETE
    }

    public TextOp {
        if (kind == null) {
            throw new IllegalArgumentException("kind is null");
        }
        if (index < 0) {
            throw new IllegalArgumentException("index is negative");
        }
        if (text == null) {
            throw new IllegalArgumentException("text is null");
        }
        if (length < 0) {
            throw new IllegalArgumentException("length is negative");
        }
        if (baseRevision < 0) {
            throw new IllegalArgumentException("base revision is negative");
        }
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("client id is blank");
        }
        if (kind == Kind.INSERT && text.isEmpty()) {
            throw new IllegalArgumentException("insert text is empty");
        }
        if (kind == Kind.INSERT && length != text.length()) {
            throw new IllegalArgumentException("insert length does not match the text");
        }
    }

    public static TextOp insert(int index, String text, int baseRevision, String clientId) {
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("insert text is empty");
        }
        return new TextOp(Kind.INSERT, index, text, text.length(), baseRevision, clientId);
    }

    public static TextOp delete(int index, int length, int baseRevision, String clientId) {
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
