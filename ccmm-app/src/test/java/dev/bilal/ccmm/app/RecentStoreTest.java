package dev.bilal.ccmm.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.bilal.ccmm.app.session.RecentEntry;
import dev.bilal.ccmm.app.session.RecentStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RecentStoreTest {
    @TempDir
    Path config;

    @TempDir
    Path projects;

    @Test
    void secondProjectOpenedIsFirstOnRead() throws Exception {
        Path first = Files.createDirectories(projects.resolve("first"));
        Path second = Files.createDirectories(projects.resolve("second"));
        RecentStore store = new RecentStore(config);
        store.rememberProject(first);
        store.rememberProject(second);
        List<RecentEntry> listed = store.projects();
        assertEquals(2, listed.size());
        assertEquals(second.toAbsolutePath().normalize(), listed.get(0).path());
        assertEquals(first.toAbsolutePath().normalize(), listed.get(1).path());
        assertFalse(listed.get(0).missing());
    }

    @Test
    void deletedPathStaysListedAndMarkedMissing() throws Exception {
        Path gone = Files.createDirectories(projects.resolve("gone"));
        RecentStore store = new RecentStore(config);
        store.rememberProject(gone);
        Files.delete(gone);
        List<RecentEntry> listed = store.projects();
        assertEquals(1, listed.size());
        assertTrue(listed.get(0).missing());
        assertEquals(gone.toAbsolutePath().normalize(), listed.get(0).path());
    }

    @Test
    void duplicateFileMovesToTopAndCapsAtTwenty() throws Exception {
        RecentStore store = new RecentStore(config);
        Path keep = Files.writeString(projects.resolve("keep.java"), "class Keep {}");
        for (int i = 0; i < 25; i++) {
            Path file = Files.writeString(projects.resolve("f" + i + ".java"), "class F" + i + " {}");
            store.rememberFile(file);
        }
        store.rememberFile(keep);
        List<RecentEntry> listed = store.files();
        assertEquals(RecentStore.MAX_FILES, listed.size());
        assertEquals(keep.toAbsolutePath().normalize(), listed.get(0).path());
    }
}
