package dev.bilal.ccmm.app.editor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.Optional;

/**
 * Caret history for Alt+Left / Alt+Right. Entries are (path, offset, line).
 * A move pushes only when the file changes or the line jumps by more than
 * {@link #LINE_THRESHOLD}. Back/forward themselves do not push.
 */
public final class NavigationHistory {
    public static final int LINE_THRESHOLD = 20;
    public static final int CAP = 50;

    public record Location(String path, int offset, int line) {
        public Location {
            Objects.requireNonNull(path, "path");
            if (path.isBlank()) {
                throw new IllegalArgumentException("path is blank");
            }
            if (offset < 0) {
                throw new IllegalArgumentException("offset < 0");
            }
            if (line < 1) {
                throw new IllegalArgumentException("line < 1");
            }
        }
    }

    private final Deque<Location> back = new ArrayDeque<>();
    private final Deque<Location> forward = new ArrayDeque<>();
    private Location last;
    private boolean navigating;

    /** Records a caret position. No-op while applying back/forward. */
    public void record(String path, int offset, int line) {
        if (navigating || path == null || path.isBlank()) {
            return;
        }
        Location here = new Location(path, Math.max(0, offset), Math.max(1, line));
        if (last == null) {
            last = here;
            return;
        }
        if (!significant(last, here)) {
            return;
        }
        back.addLast(last);
        trimBack();
        forward.clear();
        last = here;
    }

    /**
     * Pops the previous location. {@code current} is pushed onto the forward
     * stack. Does not call {@link #record}.
     */
    public Optional<Location> back(Location current) {
        if (back.isEmpty() || current == null) {
            return Optional.empty();
        }
        navigating = true;
        try {
            forward.addFirst(current);
            trimForward();
            Location target = back.removeLast();
            last = target;
            return Optional.of(target);
        } finally {
            navigating = false;
        }
    }

    public Optional<Location> forward(Location current) {
        if (forward.isEmpty() || current == null) {
            return Optional.empty();
        }
        navigating = true;
        try {
            back.addLast(current);
            trimBack();
            Location target = forward.removeFirst();
            last = target;
            return Optional.of(target);
        } finally {
            navigating = false;
        }
    }

    public boolean canGoBack() {
        return !back.isEmpty();
    }

    public boolean canGoForward() {
        return !forward.isEmpty();
    }

    public int backSize() {
        return back.size();
    }

    public Location last() {
        return last;
    }

    static boolean significant(Location from, Location to) {
        if (!from.path().equals(to.path())) {
            return true;
        }
        return Math.abs(from.line() - to.line()) > LINE_THRESHOLD;
    }

    private void trimBack() {
        while (back.size() > CAP) {
            back.removeFirst();
        }
    }

    private void trimForward() {
        while (forward.size() > CAP) {
            forward.removeLast();
        }
    }
}
