package dev.bilal.ccmm.app.settings;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

/**
 * Loads and saves settings under the OS config directory.
 * Writes go to a temp file then move, so a crash does not empty the file.
 */
public final class SettingsStore {
    public static final String FILE_NAME = "settings.properties";
    public static final String RELAY_ERROR = "Enter a ws:// or wss:// URL";

    private static final String KEY_THEME = "theme";
    private static final String KEY_FONT = "editorFontSize";
    private static final String KEY_LIGATURES = "ligatures";
    private static final String KEY_RELAY = "relayUrl";
    private static final String KEY_DISPLAY = "displayName";

    private final Path file;
    private Settings current;

    public SettingsStore(Path directory) {
        Objects.requireNonNull(directory);
        this.file = directory.resolve(FILE_NAME);
        this.current = loadFromDisk();
    }

    /** Windows %APPDATA%/ccmm, macOS Application Support/ccmm, else ~/.config/ccmm. */
    public static Path defaultDirectory() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.isBlank()) {
                return Path.of(appData, "ccmm");
            }
            return Path.of(System.getProperty("user.home"), "AppData", "Roaming", "ccmm");
        }
        if (os.contains("mac")) {
            return Path.of(System.getProperty("user.home"), "Library", "Application Support", "ccmm");
        }
        String xdg = System.getenv("XDG_CONFIG_HOME");
        if (xdg != null && !xdg.isBlank()) {
            return Path.of(xdg, "ccmm");
        }
        return Path.of(System.getProperty("user.home"), ".config", "ccmm");
    }

    public Path file() {
        return file;
    }

    public Settings current() {
        return current;
    }

    public Settings load() {
        current = loadFromDisk();
        return current;
    }

    /** Persists defaults. Caller confirms the reset dialog first. */
    public void resetToDefaults() {
        current = Settings.defaults();
        write(current);
    }

    public void setTheme(Settings.Theme theme) {
        current = current.withTheme(Objects.requireNonNull(theme));
        write(current);
    }

    /**
     * Saves font size when in 8..32. Outside that range the previous size stays.
     *
     * @return true when saved
     */
    public boolean setEditorFontSize(int size) {
        if (size < Settings.MIN_FONT_SIZE || size > Settings.MAX_FONT_SIZE) {
            return false;
        }
        current = current.withEditorFontSize(size);
        write(current);
        return true;
    }

    public void setLigatures(Settings.Ligatures ligatures) {
        current = current.withLigatures(Objects.requireNonNull(ligatures));
        write(current);
    }

    /**
     * Saves a blank or ws(s) URL. Invalid URLs leave the previous value.
     *
     * @return null on success, {@link #RELAY_ERROR} when rejected
     */
    public String setRelayUrl(String url) {
        String trimmed = url == null ? "" : url.trim();
        if (!isValidRelay(trimmed)) {
            return RELAY_ERROR;
        }
        current = current.withRelayUrl(trimmed);
        write(current);
        return null;
    }

    public void setDisplayName(String displayName) {
        String name = displayName == null || displayName.isBlank()
                ? Settings.defaults().displayName()
                : displayName.trim();
        current = current.withDisplayName(name);
        write(current);
    }

    public static boolean isValidRelay(String url) {
        if (url == null || url.isBlank()) {
            return true;
        }
        String lower = url.toLowerCase(Locale.ROOT);
        return lower.startsWith("ws://") || lower.startsWith("wss://");
    }

    private Settings loadFromDisk() {
        if (!Files.isRegularFile(file)) {
            return Settings.defaults();
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            props.load(in);
        } catch (IOException ignored) {
            return Settings.defaults();
        }
        Settings.Theme theme = Settings.Theme.fromWire(props.getProperty(KEY_THEME));
        int font = parseFont(props.getProperty(KEY_FONT));
        Settings.Ligatures ligatures = Settings.Ligatures.fromWire(props.getProperty(KEY_LIGATURES));
        String relay = props.getProperty(KEY_RELAY, "");
        if (!isValidRelay(relay)) {
            relay = "";
        }
        String display = props.getProperty(KEY_DISPLAY);
        if (display == null || display.isBlank()) {
            display = Settings.defaults().displayName();
        }
        return new Settings(theme, font, ligatures, relay, display);
    }

    private static int parseFont(String raw) {
        if (raw == null || raw.isBlank()) {
            return Settings.DEFAULT_FONT_SIZE;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            if (value < Settings.MIN_FONT_SIZE || value > Settings.MAX_FONT_SIZE) {
                return Settings.DEFAULT_FONT_SIZE;
            }
            return value;
        } catch (NumberFormatException ignored) {
            return Settings.DEFAULT_FONT_SIZE;
        }
    }

    private void write(Settings settings) {
        try {
            Files.createDirectories(file.getParent());
            Properties props = new Properties();
            props.setProperty(KEY_THEME, settings.theme().wire());
            props.setProperty(KEY_FONT, Integer.toString(settings.editorFontSize()));
            props.setProperty(KEY_LIGATURES, settings.ligatures().wire());
            props.setProperty(KEY_RELAY, settings.relayUrl());
            props.setProperty(KEY_DISPLAY, settings.displayName());
            Path temp = Files.createTempFile(file.getParent(), "settings-", ".tmp");
            try (OutputStream out = Files.newOutputStream(temp)) {
                props.store(out, "CCMM settings");
            }
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException failure) {
            throw new IllegalStateException("could not write settings: " + file, failure);
        }
    }
}
