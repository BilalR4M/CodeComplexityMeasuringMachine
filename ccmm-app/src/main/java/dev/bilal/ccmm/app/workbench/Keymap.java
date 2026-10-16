package dev.bilal.ccmm.app.workbench;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;

/**
 * Maps shortcuts to command ids. Missing commands set status
 * {@code Command is not available yet} and do not throw.
 */
public final class Keymap {
    public static final String NOT_AVAILABLE = "Command is not available yet";

    public enum Platform {
        MAC,
        OTHER
    }

    private final Platform platform;
    private final Map<Shortcut, String> bindings = new LinkedHashMap<>();

    public Keymap(Platform platform) {
        this.platform = Objects.requireNonNull(platform);
    }

    public Platform platform() {
        return platform;
    }

    public void bind(Shortcut shortcut, String commandId) {
        Objects.requireNonNull(shortcut);
        if (commandId == null || commandId.isBlank()) {
            throw new IllegalArgumentException("command id is blank");
        }
        bindings.put(shortcut, commandId);
    }

    public Optional<String> commandId(Shortcut shortcut) {
        return Optional.ofNullable(bindings.get(shortcut));
    }

    public static Platform detectPlatform() {
        String os = System.getProperty("os.name", "");
        return os.startsWith("Mac") ? Platform.MAC : Platform.OTHER;
    }

    /** Seed bindings from the UI spec. */
    public static Keymap seeded(Platform platform) {
        Keymap map = new Keymap(platform);
        map.bind(Shortcut.primary(KeyCode.B), CommandId.VIEW_TOGGLE_SIDE.value());
        map.bind(Shortcut.primary(KeyCode.J), CommandId.VIEW_TOGGLE_BOTTOM.value());
        map.bind(Shortcut.primaryAlt(KeyCode.B), CommandId.VIEW_TOGGLE_ACTIVITY.value());
        map.bind(Shortcut.primaryShift(KeyCode.E), CommandId.VIEW_EXPLORER.value());
        map.bind(Shortcut.primaryShift(KeyCode.F), CommandId.VIEW_SEARCH.value());
        map.bind(Shortcut.primaryShift(KeyCode.O), CommandId.VIEW_OUTLINE.value());
        map.bind(Shortcut.primary(KeyCode.P), CommandId.QUICK_OPEN.value());
        map.bind(Shortcut.primaryShift(KeyCode.P), CommandId.COMMAND_PALETTE.value());
        map.bind(Shortcut.primary(KeyCode.F), CommandId.FIND.value());
        map.bind(Shortcut.primary(KeyCode.H), CommandId.REPLACE.value());
        map.bind(Shortcut.primary(KeyCode.G), CommandId.GO_TO_LINE.value());
        map.bind(Shortcut.primary(KeyCode.W), CommandId.CLOSE_TAB.value());
        map.bind(Shortcut.primaryShift(KeyCode.T), CommandId.REOPEN_TAB.value());
        map.bind(Shortcut.primary(KeyCode.COMMA), CommandId.VIEW_SETTINGS.value());
        return map;
    }

    public void install(Scene scene, Workbench workbench) {
        Objects.requireNonNull(scene);
        Objects.requireNonNull(workbench);
        for (Map.Entry<Shortcut, String> entry : bindings.entrySet()) {
            KeyCodeCombination combination = entry.getKey().toCombination(platform);
            String commandId = entry.getValue();
            scene.getAccelerators().put(combination, () -> fire(workbench, commandId));
        }
    }

    /** Used by tests to invoke a binding without a Scene. */
    public void fire(Workbench workbench, String commandId) {
        if (workbench.commands().find(commandId).isEmpty()) {
            workbench.setStatus(NOT_AVAILABLE);
            return;
        }
        workbench.execute(commandId);
    }

    public KeyCodeCombination combination(Shortcut shortcut) {
        return shortcut.toCombination(platform);
    }
}
