package dev.bilal.ccmm.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.bilal.ccmm.app.document.Document;
import dev.bilal.ccmm.app.document.EditOrigin;
import dev.bilal.ccmm.app.editor.EditorTabs;
import dev.bilal.ccmm.app.editor.OpenTab;
import java.util.List;
import org.junit.jupiter.api.Test;

class DocumentModelDirtyTest {
    @Test
    void dirtyDocumentShowsUnsavedAccessibleName() {
        Document document = new Document("src/Foo.java", "class Foo {}");
        OpenTab tab = new OpenTab(document, false);
        assertEquals("Foo.java", tab.accessibleName());
        document.replace(0, 0, "//", EditOrigin.LOCAL);
        assertTrue(document.dirty());
        assertEquals("Foo.java, unsaved", tab.accessibleName());
        assertEquals("Foo.java *", tab.label());
    }

    @Test
    void previewIsReplacedByNextPreviewOpen() {
        EditorTabs tabs = new EditorTabs();
        tabs.openPreview(new Document("A.java", "a"));
        tabs.openPreview(new Document("B.java", "b"));
        assertEquals(1, tabs.tabs().size());
        assertEquals("B.java", tabs.active().orElseThrow().relativePath());
        assertTrue(tabs.active().orElseThrow().preview());
    }

    @Test
    void localPinClearsPreviewFlag() {
        EditorTabs tabs = new EditorTabs();
        tabs.openPreview(new Document("A.java", "a"));
        tabs.pinActive();
        assertFalse(tabs.active().orElseThrow().preview());
        assertEquals(List.of("A.java"), tabs.pinnedPaths());
    }

    @Test
    void previewIsExcludedFromPinnedPaths() {
        EditorTabs tabs = new EditorTabs();
        tabs.openPinned(new Document("A.java", "a"));
        tabs.openPreview(new Document("B.java", "b"));
        assertEquals(List.of("A.java"), tabs.pinnedPaths());
    }

    @Test
    void reopenStackRemembersClosedPaths() {
        EditorTabs tabs = new EditorTabs();
        tabs.openPinned(new Document("A.java", "a"));
        tabs.openPinned(new Document("B.java", "b"));
        tabs.closeActive();
        assertEquals("B.java", tabs.reopen().orElseThrow());
        assertTrue(tabs.reopen().isEmpty());
    }
}
