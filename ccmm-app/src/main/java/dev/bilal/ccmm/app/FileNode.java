package dev.bilal.ccmm.app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javafx.application.Platform;

public record FileNode(String name, boolean directory, List<FileNode> children) {
    public FileNode {
        children = List.copyOf(children);
    }

    static FileNode tree(Path root) {
        if (onJavaFxThread()) {
            throw new IllegalStateException("directory walk is on the JavaFX thread");
        }
        if (root == null || !Files.isDirectory(root)) {
            throw new IllegalArgumentException("project root is not a directory");
        }
        return read(root);
    }

    private static FileNode read(Path directory) {
        List<FileNode> children = new ArrayList<>();
        try (var listed = Files.list(directory)) {
            List<Path> entries = listed.sorted(Comparator.comparing(path -> path.getFileName().toString())).toList();
            for (Path entry : entries) {
                boolean childDirectory = Files.isDirectory(entry, LinkOption.NOFOLLOW_LINKS);
                children.add(childDirectory ? read(entry) : new FileNode(entry.getFileName().toString(), false, List.of()));
            }
        } catch (IOException exception) {
            return new FileNode(directory.getFileName().toString(), true, List.of());
        }
        return new FileNode(directory.getFileName().toString(), true, children);
    }

    private static boolean onJavaFxThread() {
        try {
            return Platform.isFxApplicationThread();
        } catch (IllegalStateException toolkitNotStarted) {
            return false;
        }
    }
}
