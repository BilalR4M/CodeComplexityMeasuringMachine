package dev.bilal.ccmm.app.session;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** Snapshot of the last workbench session. */
public final class Session {
    private final Path lastFolder;
    private final List<String> pinned;
    private final boolean sideBarVisible;
    private final boolean bottomVisible;
    private final boolean inspectorVisible;

    public Session(
            Path lastFolder,
            List<String> pinned,
            boolean sideBarVisible,
            boolean bottomVisible,
            boolean inspectorVisible) {
        this.lastFolder = lastFolder;
        this.pinned = List.copyOf(Objects.requireNonNull(pinned));
        this.sideBarVisible = sideBarVisible;
        this.bottomVisible = bottomVisible;
        this.inspectorVisible = inspectorVisible;
    }

    public static Session empty() {
        return new Session(null, List.of(), true, true, true);
    }

    public Path lastFolder() {
        return lastFolder;
    }

    public List<String> pinned() {
        return pinned;
    }

    public boolean sideBarVisible() {
        return sideBarVisible;
    }

    public boolean bottomVisible() {
        return bottomVisible;
    }

    public boolean inspectorVisible() {
        return inspectorVisible;
    }
}
