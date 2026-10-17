package dev.bilal.ccmm.app.session;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

/**
 * Persists session.properties beside settings. Existence checks belong in
 * {@link #existing(Session)} and run off the FX thread.
 */
public final class SessionStore {
    public static final String FILE_NAME = "session.properties";
    public static final int MAX_PINNED = 20;

    private static final String KEY_FOLDER = "lastFolder";
    private static final String KEY_PINNED = "pinned";
    private static final String KEY_SIDE = "sideBarVisible";
    private static final String KEY_BOTTOM = "bottomVisible";
    private static final String KEY_INSPECTOR = "inspectorVisible";

    private final Path file;

    public SessionStore(Path directory) {
        Objects.requireNonNull(directory);
        this.file = directory.resolve(FILE_NAME);
    }

    public Path file() {
        return file;
    }

    public Session load() {
        if (!Files.isRegularFile(file)) {
            return Session.empty();
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            props.load(in);
        } catch (IOException ignored) {
            return Session.empty();
        }
        String folderRaw = props.getProperty(KEY_FOLDER, "");
        Path folder = folderRaw == null || folderRaw.isBlank() ? null : Path.of(folderRaw);
        List<String> pinned = parsePinned(props.getProperty(KEY_PINNED, ""));
        return new Session(
                folder,
                pinned,
                parseBool(props.getProperty(KEY_SIDE), true),
                parseBool(props.getProperty(KEY_BOTTOM), true),
                parseBool(props.getProperty(KEY_INSPECTOR), true));
    }

    public void save(Session session) {
        Objects.requireNonNull(session);
        try {
            Files.createDirectories(file.getParent());
            Properties props = new Properties();
            if (session.lastFolder() != null) {
                props.setProperty(KEY_FOLDER, session.lastFolder().toAbsolutePath().toString());
            }
            props.setProperty(KEY_PINNED, String.join("\n", cap(session.pinned())));
            props.setProperty(KEY_SIDE, Boolean.toString(session.sideBarVisible()));
            props.setProperty(KEY_BOTTOM, Boolean.toString(session.bottomVisible()));
            props.setProperty(KEY_INSPECTOR, Boolean.toString(session.inspectorVisible()));
            Path temp = Files.createTempFile(file.getParent(), "session-", ".tmp");
            try (OutputStream out = Files.newOutputStream(temp)) {
                props.store(out, "CCMM session");
            }
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException failure) {
            throw new IllegalStateException("could not write session: " + file, failure);
        }
    }

    /**
     * Drops a missing folder and missing pinned files. Safe to call off the FX thread.
     * A missing folder becomes the welcome (empty) session layout flags preserved.
     */
    public Session existing(Session session) {
        Objects.requireNonNull(session);
        Path folder = session.lastFolder();
        if (folder == null || !Files.isDirectory(folder)) {
            return new Session(
                    null,
                    List.of(),
                    session.sideBarVisible(),
                    session.bottomVisible(),
                    session.inspectorVisible());
        }
        Path root = folder.toAbsolutePath().normalize();
        List<String> kept = new ArrayList<>();
        for (String pin : session.pinned()) {
            if (pin == null || pin.isBlank()) {
                continue;
            }
            String relative = pin.replace('\\', '/');
            if (relative.contains("..")) {
                continue;
            }
            Path filePath = root.resolve(relative).normalize();
            if (!filePath.startsWith(root)) {
                continue;
            }
            if (Files.isRegularFile(filePath)) {
                kept.add(relative);
                if (kept.size() >= MAX_PINNED) {
                    break;
                }
            }
        }
        return new Session(
                root, kept, session.sideBarVisible(), session.bottomVisible(), session.inspectorVisible());
    }

    static List<String> cap(List<String> pinned) {
        if (pinned.size() <= MAX_PINNED) {
            return pinned;
        }
        return List.copyOf(pinned.subList(pinned.size() - MAX_PINNED, pinned.size()));
    }

    private static List<String> parsePinned(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<String> pins = new ArrayList<>();
        for (String line : raw.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                pins.add(trimmed.replace('\\', '/'));
            }
            if (pins.size() >= MAX_PINNED) {
                break;
            }
        }
        return List.copyOf(pins);
    }

    private static boolean parseBool(String raw, boolean fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return Boolean.parseBoolean(raw.trim());
    }
}
