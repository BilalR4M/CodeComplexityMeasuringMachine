package dev.bilal.ccmm.collab.protocol;

/**
 * Messages shared by the IDE and the relay. There is no socket in this type.
 * A file-room text op names the project-relative path and the revision it was based on.
 */
public sealed interface Message {
    record Join(String displayName, String inviteToken) implements Message {
        public Join {
            requireName(displayName);
            if (inviteToken == null || inviteToken.isBlank()) {
                throw new IllegalArgumentException("invite token is blank");
            }
        }
    }

    record Presence(String displayName, boolean online) implements Message {
        public Presence {
            requireName(displayName);
        }
    }

    record Chat(String displayName, String text) implements Message {
        public Chat {
            requireName(displayName);
            if (text == null) {
                throw new IllegalArgumentException("chat text is null");
            }
        }
    }

    record Insert(String path, int index, String text, int baseRevision, String clientId) implements Message {
        public Insert {
            requirePath(path);
            TextOp.insert(index, text, baseRevision, clientId);
        }

        public TextOp op() {
            return TextOp.insert(index, text, baseRevision, clientId);
        }
    }

    record Delete(String path, int index, int length, int baseRevision, String clientId) implements Message {
        public Delete {
            requirePath(path);
            TextOp.delete(index, length, baseRevision, clientId);
        }

        public TextOp op() {
            return TextOp.delete(index, length, baseRevision, clientId);
        }
    }

    private static void requireName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("display name is blank");
        }
    }

    private static void requirePath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("path is blank");
        }
    }
}
