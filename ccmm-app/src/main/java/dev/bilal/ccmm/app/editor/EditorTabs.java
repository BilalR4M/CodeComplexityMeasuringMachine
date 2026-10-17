package dev.bilal.ccmm.app.editor;

import dev.bilal.ccmm.app.document.Document;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Open-tab list with one preview slot. Preview is replaced by the next preview
 * open; pin comes from double-click or a LOCAL edit.
 */
public final class EditorTabs {
    public static final int REOPEN_LIMIT = 20;

    private final List<OpenTab> tabs = new ArrayList<>();
    private final Deque<String> closed = new ArrayDeque<>();
    private int active = -1;

    public List<OpenTab> tabs() {
        return List.copyOf(tabs);
    }

    public Optional<OpenTab> active() {
        return active >= 0 && active < tabs.size() ? Optional.of(tabs.get(active)) : Optional.empty();
    }

    public int activeIndex() {
        return active;
    }

    public boolean isEmpty() {
        return tabs.isEmpty();
    }

    /** Opens or replaces the preview tab. Already-open paths activate instead. */
    public void openPreview(Document document) {
        Objects.requireNonNull(document);
        int existing = indexOf(document.relativePath());
        if (existing >= 0) {
            active = existing;
            return;
        }
        int preview = previewIndex();
        OpenTab tab = new OpenTab(document, true);
        if (preview >= 0) {
            tabs.set(preview, tab);
            active = preview;
        } else {
            tabs.add(tab);
            active = tabs.size() - 1;
        }
    }

    /** Opens a pinned tab, or pins an existing preview of the same path. */
    public void openPinned(Document document) {
        Objects.requireNonNull(document);
        int existing = indexOf(document.relativePath());
        if (existing >= 0) {
            tabs.get(existing).pin();
            active = existing;
            return;
        }
        int preview = previewIndex();
        if (preview >= 0 && tabs.get(preview).relativePath().equals(document.relativePath())) {
            tabs.get(preview).pin();
            active = preview;
            return;
        }
        if (preview >= 0) {
            // leave other preview; add pinned
        }
        tabs.add(new OpenTab(document, false));
        active = tabs.size() - 1;
    }

    public void pinActive() {
        active().ifPresent(OpenTab::pin);
    }

    public void select(int index) {
        if (index < 0 || index >= tabs.size()) {
            throw new IllegalArgumentException("tab index " + index);
        }
        active = index;
    }

    public void selectPath(String relativePath) {
        int index = indexOf(relativePath);
        if (index >= 0) {
            active = index;
        }
    }

    /**
     * Removes the active tab. Caller handles dirty prompts before calling.
     *
     * @return closed relative path
     */
    public Optional<String> closeActive() {
        if (active < 0) {
            return Optional.empty();
        }
        return closeAt(active);
    }

    public Optional<String> closeAt(int index) {
        if (index < 0 || index >= tabs.size()) {
            return Optional.empty();
        }
        OpenTab removed = tabs.remove(index);
        rememberClosed(removed.relativePath());
        if (tabs.isEmpty()) {
            active = -1;
        } else if (active >= tabs.size()) {
            active = tabs.size() - 1;
        } else if (index < active) {
            active--;
        }
        return Optional.of(removed.relativePath());
    }

    public Optional<String> reopen() {
        String path = closed.pollFirst();
        return Optional.ofNullable(path);
    }

    public List<String> closedPaths() {
        return List.copyOf(closed);
    }

    /** Pinned relative paths only (preview excluded from session pin lists). */
    public List<String> pinnedPaths() {
        List<String> paths = new ArrayList<>();
        for (OpenTab tab : tabs) {
            if (!tab.preview()) {
                paths.add(tab.relativePath());
            }
        }
        return List.copyOf(paths);
    }

    private void rememberClosed(String path) {
        closed.remove(path);
        closed.addFirst(path);
        while (closed.size() > REOPEN_LIMIT) {
            closed.removeLast();
        }
    }

    private int previewIndex() {
        for (int i = 0; i < tabs.size(); i++) {
            if (tabs.get(i).preview()) {
                return i;
            }
        }
        return -1;
    }

    private int indexOf(String relativePath) {
        for (int i = 0; i < tabs.size(); i++) {
            if (tabs.get(i).relativePath().equals(relativePath)) {
                return i;
            }
        }
        return -1;
    }
}
