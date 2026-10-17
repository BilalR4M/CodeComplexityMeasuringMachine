package dev.bilal.ccmm.app.editor;

import dev.bilal.ccmm.app.document.Document;
import java.util.Objects;

/** One editor tab: a document that is either preview or pinned. */
public final class OpenTab {
    private final Document document;
    private boolean preview;

    public OpenTab(Document document, boolean preview) {
        this.document = Objects.requireNonNull(document);
        this.preview = preview;
    }

    public Document document() {
        return document;
    }

    public boolean preview() {
        return preview;
    }

    public void pin() {
        preview = false;
    }

    public String relativePath() {
        return document.relativePath();
    }

    public String fileName() {
        String path = document.relativePath();
        int slash = path.lastIndexOf('/');
        return slash < 0 ? path : path.substring(slash + 1);
    }

    /** Accessible name: {@code Foo.java} or {@code Foo.java, unsaved}. */
    public String accessibleName() {
        return document.dirty() ? fileName() + ", unsaved" : fileName();
    }

    public String label() {
        return document.dirty() ? fileName() + " *" : fileName();
    }
}
