package dev.bilal.ccmm.app.workbench;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

/** Key + modifiers. Primary is Cmd (meta) on Mac and Ctrl elsewhere. */
public record Shortcut(KeyCode key, boolean shift, boolean alt, boolean primary) {
    public Shortcut {
        if (key == null) {
            throw new IllegalArgumentException("key is null");
        }
    }

    public static Shortcut primary(KeyCode key) {
        return new Shortcut(key, false, false, true);
    }

    public static Shortcut primaryShift(KeyCode key) {
        return new Shortcut(key, true, false, true);
    }

    public static Shortcut primaryAlt(KeyCode key) {
        return new Shortcut(key, false, true, true);
    }

    /**
     * JavaFX order: shift, control, alt, meta, shortcut.
     * Mac primary → meta; other primary → control.
     */
    public KeyCodeCombination toCombination(Keymap.Platform platform) {
        KeyCombination.ModifierValue down = KeyCombination.ModifierValue.DOWN;
        KeyCombination.ModifierValue up = KeyCombination.ModifierValue.UP;
        KeyCombination.ModifierValue shiftMod = shift ? down : up;
        KeyCombination.ModifierValue altMod = alt ? down : up;
        KeyCombination.ModifierValue controlMod =
                primary && platform != Keymap.Platform.MAC ? down : up;
        KeyCombination.ModifierValue metaMod =
                primary && platform == Keymap.Platform.MAC ? down : up;
        return new KeyCodeCombination(key, shiftMod, controlMod, altMod, metaMod, up);
    }

    /** Human label for the palette, e.g. {@code Ctrl+Shift+P} or {@code Cmd+P}. */
    public String label(Keymap.Platform platform) {
        StringBuilder text = new StringBuilder();
        if (primary) {
            text.append(platform == Keymap.Platform.MAC ? "Cmd" : "Ctrl").append('+');
        }
        if (shift) {
            text.append("Shift+");
        }
        if (alt) {
            text.append("Alt+");
        }
        text.append(key.getName().length() == 1 ? key.getName().toUpperCase() : key.getName());
        return text.toString();
    }
}
