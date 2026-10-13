package dev.bilal.ccmm.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileNodeTest {
    @TempDir
    Path root;

    @Test
    void aDirectoryBecomesATreeOfItsFiles() throws Exception {
        Files.createDirectories(root.resolve("src"));
        Files.writeString(root.resolve("src/A.java"), "class A {}");
        Files.writeString(root.resolve("README.md"), "hi");

        FileNode tree = FileNode.tree(root);

        assertEquals(root.getFileName().toString(), tree.name());
        assertEquals(List.of("README.md", "src"), tree.children().stream().map(FileNode::name).toList());
        assertTrue(tree.children().get(1).directory());
        assertEquals(List.of("A.java"), tree.children().get(1).children().stream().map(FileNode::name).toList());
    }

    @Test
    void aMissingDirectoryIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> FileNode.tree(root.resolve("missing")));
    }
}
