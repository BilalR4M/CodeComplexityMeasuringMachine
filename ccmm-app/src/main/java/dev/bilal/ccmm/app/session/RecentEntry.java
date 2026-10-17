package dev.bilal.ccmm.app.session;

import java.nio.file.Path;
import java.util.Objects;

/** One absolute path in a recent list. {@code missing} is set when the path is gone on disk. */
public record RecentEntry(Path path, boolean missing) {
    public RecentEntry {
        Objects.requireNonNull(path, "path");
    }
}
