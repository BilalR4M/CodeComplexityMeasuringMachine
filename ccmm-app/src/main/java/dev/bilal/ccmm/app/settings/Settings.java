package dev.bilal.ccmm.app.settings;

import java.util.Objects;

/** Immutable settings snapshot. Theme names match ADR-0013 (dark|light). */
public final class Settings {
    public static final int DEFAULT_FONT_SIZE = 13;
    public static final int MIN_FONT_SIZE = 8;
    public static final int MAX_FONT_SIZE = 32;

    public enum Theme {
        DARK("dark"),
        LIGHT("light");

        private final String wire;

        Theme(String wire) {
            this.wire = wire;
        }

        public String wire() {
            return wire;
        }

        public String label() {
            return this == DARK ? "Dark" : "Light";
        }

        public static Theme fromWire(String value) {
            if (value == null || value.isBlank()) {
                return DARK;
            }
            for (Theme theme : values()) {
                if (theme.wire.equalsIgnoreCase(value.trim())) {
                    return theme;
                }
            }
            return DARK;
        }
    }

    public enum Ligatures {
        OFF("off"),
        ON("on");

        private final String wire;

        Ligatures(String wire) {
            this.wire = wire;
        }

        public String wire() {
            return wire;
        }

        public String label() {
            return this == OFF ? "Off" : "On";
        }

        public static Ligatures fromWire(String value) {
            if (value == null || value.isBlank()) {
                return OFF;
            }
            for (Ligatures ligatures : values()) {
                if (ligatures.wire.equalsIgnoreCase(value.trim())) {
                    return ligatures;
                }
            }
            return OFF;
        }
    }

    private final Theme theme;
    private final int editorFontSize;
    private final Ligatures ligatures;
    private final String relayUrl;
    private final String displayName;

    public Settings(Theme theme, int editorFontSize, Ligatures ligatures, String relayUrl, String displayName) {
        this.theme = Objects.requireNonNull(theme);
        this.editorFontSize = editorFontSize;
        this.ligatures = Objects.requireNonNull(ligatures);
        this.relayUrl = relayUrl == null ? "" : relayUrl;
        this.displayName = Objects.requireNonNull(displayName);
    }

    public static Settings defaults() {
        return new Settings(Theme.DARK, DEFAULT_FONT_SIZE, Ligatures.OFF, "", System.getProperty("user.name", "User"));
    }

    public Theme theme() {
        return theme;
    }

    public int editorFontSize() {
        return editorFontSize;
    }

    public Ligatures ligatures() {
        return ligatures;
    }

    public String relayUrl() {
        return relayUrl;
    }

    public String displayName() {
        return displayName;
    }

    public Settings withTheme(Theme theme) {
        return new Settings(theme, editorFontSize, ligatures, relayUrl, displayName);
    }

    public Settings withEditorFontSize(int editorFontSize) {
        return new Settings(theme, editorFontSize, ligatures, relayUrl, displayName);
    }

    public Settings withLigatures(Ligatures ligatures) {
        return new Settings(theme, editorFontSize, ligatures, relayUrl, displayName);
    }

    public Settings withRelayUrl(String relayUrl) {
        return new Settings(theme, editorFontSize, ligatures, relayUrl, displayName);
    }

    public Settings withDisplayName(String displayName) {
        return new Settings(theme, editorFontSize, ligatures, relayUrl, displayName);
    }
}
