package dev.bilal.ccmm.app;

import dev.bilal.ccmm.collab.protocol.Message;
import dev.bilal.ccmm.collab.protocol.PeerPath;
import dev.bilal.ccmm.collab.protocol.TextOp;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.fxmisc.richtext.CodeArea;
import org.reactfx.Guard;
import org.reactfx.SuspendableYes;

/**
 * Applies relay text ops for the open file. Several ops that arrive in one
 * JavaFX frame become one edit, and that edit is not an undo entry.
 */
final class RemoteEdits {
    private RemoteEdits() {}

    static List<TextOp> accepted(Path projectRoot, String openPath, List<Message> messages) {
        if (projectRoot == null || openPath == null || messages == null) {
            return List.of();
        }
        List<TextOp> ops = new ArrayList<>();
        for (Message message : messages) {
            String path = path(message);
            TextOp op = op(message);
            if (path == null || op == null) {
                continue;
            }
            try {
                if (openPath.equals(PeerPath.require(projectRoot, path))) {
                    ops.add(op);
                }
            } catch (IllegalArgumentException ignored) {
                // absolute, parent segment, or a link that leaves the project
            }
        }
        return ops;
    }

    static void apply(CodeArea editor, SuspendableYes localUndo, List<TextOp> ops) {
        if (editor == null || localUndo == null || ops == null || ops.isEmpty()) {
            return;
        }
        String current = editor.getText();
        String updated = fold(current, ops);
        if (updated.equals(current)) {
            return;
        }
        int caret = caretAfter(editor.getCaretPosition(), current, ops);
        var batch = editor.createMultiChange(1);
        batch.replaceText(0, current.length(), updated);
        try (Guard ignored = localUndo.suspend()) {
            batch.commit();
        }
        editor.moveTo(Math.min(caret, editor.getLength()));
    }

    private static String fold(String text, List<TextOp> ops) {
        String current = text;
        for (TextOp op : ops) {
            current = step(current, op);
        }
        return current;
    }

    private static int caretAfter(int caret, String text, List<TextOp> ops) {
        int position = caret;
        String current = text;
        for (TextOp op : ops) {
            String next = step(current, op);
            if (next.equals(current)) {
                continue;
            }
            if (op.kind() == TextOp.Kind.INSERT && op.index() <= position) {
                position += op.text().length();
            } else if (op.kind() == TextOp.Kind.DELETE && op.index() < position) {
                int removed = Math.min(op.length(), position - op.index());
                position -= removed;
            }
            current = next;
        }
        return Math.max(0, position);
    }

    private static String step(String text, TextOp op) {
        if (op.kind() == TextOp.Kind.INSERT) {
            if (op.index() > text.length()) {
                return text;
            }
            return text.substring(0, op.index()) + op.text() + text.substring(op.index());
        }
        int end = op.index() + op.length();
        if (op.length() == 0 || op.index() > text.length() || end > text.length()) {
            return text;
        }
        return text.substring(0, op.index()) + text.substring(end);
    }

    private static String path(Message message) {
        return switch (message) {
            case Message.Insert insert -> insert.path();
            case Message.Delete delete -> delete.path();
            default -> null;
        };
    }

    private static TextOp op(Message message) {
        return switch (message) {
            case Message.Insert insert -> insert.op();
            case Message.Delete delete -> delete.op();
            default -> null;
        };
    }
}
