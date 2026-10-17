package dev.bilal.ccmm.app.document;

import java.util.Objects;

/**
 * One open buffer. Relative paths use {@code /}. Hex and UI live elsewhere;
 * this type only tracks text, dirty, line ending, and edit origin.
 *
 * <p>Until ADR-0010 is accepted, callers should reject non-{@code .java} paths
 * with {@link #ONLY_JAVA_MESSAGE} before constructing a document. The path field
 * still accepts any string so that ADR is a one-line policy change.
 *
 * <p>In-memory text is always LF. {@link LineEnding} is the preference for the
 * next save.
 */
public final class Document {
    public static final String ONLY_JAVA_MESSAGE = "Only Java files can be opened";
    /** Same 1 MiB cap as {@code ProjectScanner}; over this, the editor skips analysis and highlighting. */
    public static final long LARGE_FILE_BYTES = 1024L * 1024L;
    public static final String LARGE_FILE_BANNER = "Large file — highlighting is off";

    private final String relativePath;
    private String text;
    private String saved;
    private LineEnding lineEnding;
    private LineEnding savedLineEnding;
    private boolean largeFile;

    public Document(String relativePath, String text) {
        this(relativePath, text, LineEnding.detect(text));
    }

    public Document(String relativePath, String text, LineEnding lineEnding) {
        this(relativePath, text, lineEnding, -1L);
    }

    /**
     * @param knownByteSize file size before read when known; {@code -1} if unknown
     */
    public Document(String relativePath, String text, LineEnding lineEnding, long knownByteSize) {
        this.relativePath = normalizePath(relativePath);
        this.lineEnding = lineEnding == null ? LineEnding.LF : lineEnding;
        this.savedLineEnding = this.lineEnding;
        String raw = text == null ? "" : text;
        this.text = LineEnding.toLf(raw);
        this.saved = this.text;
        this.largeFile = isLarge(this.text, knownByteSize);
    }

    /** True when disk bytes or in-memory length exceed {@link #LARGE_FILE_BYTES}. */
    public static boolean isLarge(String text, long knownByteSize) {
        if (knownByteSize > LARGE_FILE_BYTES) {
            return true;
        }
        return text != null && text.length() > LARGE_FILE_BYTES;
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

    public LineEnding lineEnding() {
        return lineEnding;
    }

    /** True when this buffer is over the large-file threshold (E2-T09). */
    public boolean largeFile() {
        return largeFile;
    }

    /**
     * Sets the ending used on the next save. Switching away from the saved
     * ending marks the document dirty.
     */
    public void setLineEnding(LineEnding ending) {
        this.lineEnding = ending == null ? LineEnding.LF : ending;
    }

    public boolean dirty() {
        return !text.equals(saved) || lineEnding != savedLineEnding;
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
        refreshLargeFileFlag();
        if (origin == EditOrigin.REMOTE && text.equals(saved) && lineEnding == savedLineEnding) {
            // restored to disk snapshot — not dirty
            return;
        }
        // LOCAL always recomputes; REMOTE that diverges from saved stays dirty
    }

    /** Records the current text and line ending as the last disk snapshot. */
    public void markSaved() {
        saved = text;
        savedLineEnding = lineEnding;
    }

    /** Replaces the buffer from disk and clears dirty. */
    public void reload(String diskText) {
        reload(diskText, LineEnding.detect(diskText));
    }

    public void reload(String diskText, LineEnding ending) {
        lineEnding = ending == null ? LineEnding.LF : ending;
        savedLineEnding = lineEnding;
        text = LineEnding.toLf(diskText);
        saved = text;
        refreshLargeFileFlag();
    }

    private void refreshLargeFileFlag() {
        largeFile = isLarge(text, -1L);
    }

    /** Bytes to write on save (UTF-8 string with the chosen ending). */
    public String diskText() {
        return lineEnding.encode(text);
    }
}
