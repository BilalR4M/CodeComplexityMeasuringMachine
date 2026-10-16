package dev.bilal.ccmm.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.bilal.ccmm.app.document.Document;
import dev.bilal.ccmm.app.document.EditOrigin;
import org.junit.jupiter.api.Test;

class DocumentTest {
    @Test
    void localReplaceMarksDirty() {
        Document document = new Document("src/A.java", "class A {}");
        assertFalse(document.dirty());
        document.replace(6, 7, "B", EditOrigin.LOCAL);
        assertEquals("class B {}", document.text());
        assertTrue(document.dirty());
    }

    @Test
    void remoteReplaceThatRestoresSavedClearsDirty() {
        Document document = new Document("A.java", "ab");
        document.replace(0, 2, "Xab", EditOrigin.LOCAL);
        assertTrue(document.dirty());
        document.replace(0, 3, "ab", EditOrigin.REMOTE);
        assertEquals("ab", document.text());
        assertFalse(document.dirty());
    }

    @Test
    void remoteReplaceThatDivergesStaysDirty() {
        Document document = new Document("A.java", "ab");
        document.replace(0, 0, "X", EditOrigin.REMOTE);
        assertEquals("Xab", document.text());
        assertTrue(document.dirty());
    }

    @Test
    void emptyTextIsValid() {
        Document document = new Document("Empty.java", "");
        assertEquals("", document.text());
        assertFalse(document.dirty());
        document.replace(0, 0, "x", EditOrigin.LOCAL);
        assertTrue(document.dirty());
        document.replace(0, 1, "", EditOrigin.REMOTE);
        assertFalse(document.dirty());
    }

    @Test
    void pathsNormalizeBackslashes() {
        Document document = new Document("src\\main\\A.java", "x");
        assertEquals("src/main/A.java", document.relativePath());
    }

    @Test
    void outOfBoundsReplaceThrows() {
        Document document = new Document("A.java", "ab");
        assertThrows(IllegalArgumentException.class, () -> document.replace(-1, 0, "x", EditOrigin.LOCAL));
        assertThrows(IllegalArgumentException.class, () -> document.replace(0, 3, "x", EditOrigin.LOCAL));
        assertThrows(IllegalArgumentException.class, () -> document.replace(2, 1, "x", EditOrigin.LOCAL));
    }

    @Test
    void javaPathPolicy() {
        assertTrue(Document.isJavaPath("A.java"));
        assertTrue(Document.isJavaPath("src\\B.JAVA"));
        assertFalse(Document.isJavaPath("notes.txt"));
        assertFalse(Document.isJavaPath("A.java.md"));
    }

    @Test
    void markSavedClearsDirty() {
        Document document = new Document("A.java", "a");
        document.replace(0, 1, "b", EditOrigin.LOCAL);
        assertTrue(document.dirty());
        document.markSaved();
        assertFalse(document.dirty());
    }
}
