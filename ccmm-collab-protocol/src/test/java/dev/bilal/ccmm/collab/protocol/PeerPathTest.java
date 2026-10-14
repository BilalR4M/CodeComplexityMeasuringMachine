package dev.bilal.ccmm.collab.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PeerPathTest {
    @TempDir
    Path root;

    @Test
    void anAbsolutePathIsRejected() {
        String absolute = root.resolve("A.java").toAbsolutePath().toString();

        assertThrows(IllegalArgumentException.class, () -> PeerPath.require(root, absolute));
    }

    @Test
    void aParentSegmentIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> PeerPath.require(root, "../A.java"));
        assertThrows(IllegalArgumentException.class, () -> PeerPath.require(root, "src/../A.java"));
    }

    @Test
    void aRelativePathInsideTheRootIsAccepted() throws Exception {
        Files.createDirectories(root.resolve("src"));
        Files.writeString(root.resolve("src/A.java"), "class A {}");

        assertEquals("src/A.java", PeerPath.require(root, "src/A.java"));
        assertEquals("src/A.java", PeerPath.require(root, "src\\A.java"));
        assertEquals("dir/New.java", PeerPath.require(root, "dir/New.java"));
    }

    @Test
    void aSymlinkThatLeavesTheRootIsRejected() throws Exception {
        Path outside = Files.createTempDirectory("ccmm-peer-outside");
        try {
            Files.writeString(outside.resolve("Secret.java"), "class Secret {}");
            Path link = root.resolve("escape");
            if (!linkTo(link, outside)) {
                Assumptions.abort("cannot create a directory link");
            }
            Files.writeString(root.resolve("A.java"), "class A {}");

            assertThrows(IllegalArgumentException.class, () -> PeerPath.require(root, "escape/Secret.java"));
            assertEquals("A.java", PeerPath.require(root, "A.java"));
        } finally {
            Files.deleteIfExists(root.resolve("escape"));
            deleteTree(outside);
        }
    }

    private static boolean linkTo(Path link, Path target) throws Exception {
        try {
            Files.createSymbolicLink(link, target);
            return true;
        } catch (FileSystemException exception) {
            Process process = new ProcessBuilder("cmd", "/c", "mklink", "/J", link.toString(), target.toString())
                    .redirectErrorStream(true)
                    .start();
            process.waitFor();
            return Files.exists(link);
        }
    }

    private static void deleteTree(Path directory) throws Exception {
        if (!Files.exists(directory)) {
            return;
        }
        try (var walk = Files.walk(directory)) {
            walk.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (java.io.IOException ignored) {
                    // best-effort cleanup of the sibling directory
                }
            });
        }
    }
}
