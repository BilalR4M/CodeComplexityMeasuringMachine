package dev.bilal.ccmm.core;

import java.io.IOException;
import java.nio.file.FileSystemLoopException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class ProjectScanner {
    static final long MAX_FILE_BYTES = 1024L * 1024L;

    private final Analyzer analyzer;
    private final Path root;
    private final Path rootReal;
    private final List<AnalysisResult> files = new ArrayList<>();
    private final List<String> skipped = new ArrayList<>();
    private final Set<Path> seenDirectories = new HashSet<>();

    private ProjectScanner(Analyzer analyzer, Path root, Path rootReal) {
        this.analyzer = analyzer;
        this.root = root;
        this.rootReal = rootReal;
    }

    static ProjectReport scan(Analyzer analyzer, Path projectRoot) {
        if (projectRoot == null) {
            throw new IllegalArgumentException("project root is null");
        }
        Path root = projectRoot.toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            throw new IllegalArgumentException("project root is not a directory");
        }
        Path rootReal;
        try {
            rootReal = root.toRealPath();
        } catch (IOException exception) {
            throw new IllegalArgumentException("project root is not a directory", exception);
        }
        ProjectScanner scanner = new ProjectScanner(analyzer, root, rootReal);
        try {
            Files.walkFileTree(root, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, scanner.visitor());
        } catch (IOException exception) {
            throw new IllegalStateException("project scan failed", exception);
        }
        scanner.files.sort(Comparator.comparing(ProjectScanner::relativePath));
        scanner.skipped.sort(Comparator.naturalOrder());
        return new ProjectReport(scanner.files, scanner.skipped);
    }

    private FileVisitor<Path> visitor() {
        return new FileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                return enterDirectory(dir);
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                consider(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exception) {
                if (!(exception instanceof FileSystemLoopException)) {
                    skipped.add(relative(file));
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exception) {
                return FileVisitResult.CONTINUE;
            }
        };
    }

    private FileVisitResult enterDirectory(Path dir) {
        try {
            Path real = dir.toRealPath();
            if (!insideRoot(real)) {
                skipped.add(relative(dir));
                return FileVisitResult.SKIP_SUBTREE;
            }
            if (!seenDirectories.add(real)) {
                return FileVisitResult.SKIP_SUBTREE;
            }
        } catch (IOException exception) {
            skipped.add(relative(dir));
            return FileVisitResult.SKIP_SUBTREE;
        }
        return FileVisitResult.CONTINUE;
    }

    private void consider(Path file) {
        if (!file.getFileName().toString().endsWith(".java")) {
            return;
        }
        try {
            if (Files.isSymbolicLink(file)) {
                Path real = file.toRealPath();
                if (!insideRoot(real)) {
                    skipped.add(relative(file));
                    return;
                }
            }
            if (Files.size(file) > MAX_FILE_BYTES) {
                skipped.add(relative(file));
                return;
            }
            files.add(analyzer.analyze(Files.readString(file), relative(file)));
        } catch (IOException exception) {
            files.add(new AnalysisResult.Failed(relative(file), exception.getMessage()));
        }
    }

    private boolean insideRoot(Path real) {
        Path relative = rootReal.relativize(real.normalize());
        if (relative.isAbsolute()) {
            return false;
        }
        for (Path part : relative) {
            if ("..".equals(part.toString())) {
                return false;
            }
        }
        return true;
    }

    private String relative(Path path) {
        Path absolute = path.toAbsolutePath().normalize();
        return root.relativize(absolute).toString().replace('\\', '/');
    }

    private static String relativePath(AnalysisResult result) {
        return switch (result) {
            case AnalysisResult.Ok ok -> ok.relativePath();
            case AnalysisResult.Failed failed -> failed.relativePath();
        };
    }
}
