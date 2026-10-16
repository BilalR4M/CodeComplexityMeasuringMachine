package dev.bilal.ccmm.app.document;

import java.util.Objects;

/**
 * One open buffer. Relative paths use {@code /}. Hex and UI live elsewhere;
 * this type only tracks text, dirty, and edit origin.
 *
 * <p>Until ADR-0010 is accepted, callers should reject non-{@code .java} paths
 * with {@link #ONLY_JAVA_MESSAGE} before constructing a document. The path field
 * still accepts any string so that ADR is a one-line policy change.
 */
public final class Document {
    public static final String ONLY_JAVA_MESSAGE = "Only Java files can be opened";

    private final String relativePath;
    private String text;
    private String saved;

    public Document(String relativePath, String text) {
        this.relativePath = normalizePath(relativePath);
        this.text = text == null ? "" : text;
        this.saved = this.text;
    }

    public static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("path is blank");
        }
        return path.replace('\\', '/');
    }

    /** True when the path is a Java source file (ADR-0010 still proposed). */
    public static boolean isJavaPath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return false;
        }
        String normalized = relativePath.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        String name = slash < 0 ? normalized : normalized.substring(slash + 1);
        return name.toLowerCase().endsWith(".java");
    }

    public String relativePath() {
        return relativePath;
    }

    public String text() {
        return text;
    }

    public boolean dirty() {
        return !text.equals(saved);
    }

    /**
     * Replaces {@code [start, end)} with {@code insertion}. Bounds errors throw
     * {@link IllegalArgumentException} to the caller (workbench sets status).
     *
     * <p>{@link EditOrigin#LOCAL} marks dirty when the buffer differs from the
     * last saved snapshot. {@link EditOrigin#REMOTE} clears dirty when the
     * result matches the saved snapshot.
     */
    public void replace(int start, int end, String insertion, EditOrigin origin) {
        Objects.requireNonNull(origin);
        String insert = insertion == null ? "" : insertion;
        if (start < 0 || end < start || end > text.length()) {
            throw new IllegalArgumentException(
                    "replace bounds [" + start + ", " + end + ") for length " + text.length());
        }
        text = text.substring(0, start) + insert + text.substring(end);
        if (origin == EditOrigin.REMOTE && text.equals(saved)) {
            // restored to disk snapshot — not dirty
            return;
        }
        // LOCAL always recomputes; REMOTE that diverges from saved stays dirty
    }

    /** Records the current text as the last disk snapshot (after load or save). */
    public void markSaved() {
        saved = text;
    }

    /** Replaces the buffer from disk and clears dirty. */
    public void reload(String diskText) {
        text = diskText == null ? "" : diskText;
        saved = text;
    }
}
