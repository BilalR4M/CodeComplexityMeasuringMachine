package dev.bilal.ccmm.app.document;

/** Disk line-ending preference for a buffer. In-memory text is always LF. */
public enum LineEnding {
    LF("LF"),
    CRLF("CRLF");

    private final String label;

    LineEnding(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** CRLF if any {@code \r\n} pair exists, otherwise LF. */
    public static LineEnding detect(String text) {
        if (text != null && text.contains("\r\n")) {
            return CRLF;
        }
        return LF;
    }

    /** Normalize to LF for the editor buffer. */
    public static String toLf(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return text.replace("\r\n", "\n").replace("\r", "\n");
    }

    /** Encode an LF buffer for disk. */
    public String encode(String lfText) {
        String body = toLf(lfText);
        return this == CRLF ? body.replace("\n", "\r\n") : body;
    }
}
