package dev.bilal.ccmm.app.session;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Persists recent projects and files as plain text beside settings.
 * Absolute paths only; never sent to the relay. Missing paths stay listed.
 */
public final class RecentStore {
    public static final String PROJECTS_FILE = "recent-projects.txt";
    public static final String FILES_FILE = "recent-files.txt";
    public static final int MAX_PROJECTS = 10;
    public static final int MAX_FILES = 20;

    private final Path directory;

    public RecentStore(Path directory) {
        this.directory = Objects.requireNonNull(directory);
    }

    public Path directory() {
        return directory;
    }

    public Path projectsFile() {
        return directory.resolve(PROJECTS_FILE);
    }

    public Path filesFile() {
        return directory.resolve(FILES_FILE);
    }

    /** Most recent first. Marks entries whose path no longer exists. */
    public List<RecentEntry> projects() {
        return read(projectsFile(), true);
    }

    public List<RecentEntry> files() {
        return read(filesFile(), false);
    }

    /** Moves {@code project} to the top (cap {@link #MAX_PROJECTS}). */
    public void rememberProject(Path project) {
        if (project == null) {
            return;
        }
        write(projectsFile(), prepend(loadPaths(projectsFile()), project.toAbsolutePath().normalize()), MAX_PROJECTS);
    }

    /** Moves {@code file} to the top (cap {@link #MAX_FILES}). */
    public void rememberFile(Path file) {
        if (file == null) {
            return;
        }
        write(filesFile(), prepend(loadPaths(filesFile()), file.toAbsolutePath().normalize()), MAX_FILES);
    }

    private List<RecentEntry> read(Path file, boolean directoryExpected) {
        List<RecentEntry> entries = new ArrayList<>();
        for (Path path : loadPaths(file)) {
            boolean missing = directoryExpected ? !Files.isDirectory(path) : !Files.isRegularFile(path);
            entries.add(new RecentEntry(path, missing));
        }
        return List.copyOf(entries);
    }

    private static List<Path> loadPaths(Path file) {
        if (!Files.isRegularFile(file)) {
            return List.of();
        }
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            List<Path> paths = new ArrayList<>();
            for (String line : lines) {
                if (line == null || line.isBlank()) {
                    continue;
                }
                paths.add(Path.of(line.strip()).toAbsolutePath().normalize());
            }
            return paths;
        } catch (IOException ignored) {
            return List.of();
        }
    }

    private static List<Path> prepend(List<Path> existing, Path next) {
        List<Path> ordered = new ArrayList<>();
        ordered.add(next);
        for (Path path : existing) {
            if (!path.equals(next)) {
                ordered.add(path);
            }
        }
        return ordered;
    }

    private void write(Path target, List<Path> paths, int cap) {
        try {
            Files.createDirectories(directory);
            List<Path> capped = paths.size() <= cap ? paths : paths.subList(0, cap);
            StringBuilder text = new StringBuilder();
            for (Path path : capped) {
                text.append(path).append('\n');
            }
            Path temp = Files.createTempFile(directory, "recent-", ".tmp");
            Files.writeString(temp, text.toString(), StandardCharsets.UTF_8);
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException failure) {
            throw new IllegalStateException("could not write recent list: " + target, failure);
        }
    }
}
