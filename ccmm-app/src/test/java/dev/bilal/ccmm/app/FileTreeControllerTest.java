package dev.bilal.ccmm.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileTreeControllerTest {
    @TempDir
    Path root;

    @Test
    void theWalkRunsOnTheFileTreeThread() throws Exception {
        Files.writeString(root.resolve("A.java"), "class A {}");
        FileTreeController controller = new FileTreeController();
        AtomicReference<String> walkedOn = new AtomicReference<>();
        AtomicReference<FileNode> listed = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        controller.open(root, node -> {
            walkedOn.set(Thread.currentThread().getName());
            listed.set(node);
            done.countDown();
        });

        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertEquals("ccmm-file-tree", walkedOn.get());
        assertNotEquals(Thread.currentThread().getName(), walkedOn.get());
        assertEquals(List.of("A.java"), listed.get().children().stream().map(FileNode::name).toList());
        controller.close();
    }
}
