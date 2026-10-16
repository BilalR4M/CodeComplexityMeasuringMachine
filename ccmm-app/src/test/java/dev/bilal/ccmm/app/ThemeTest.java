package dev.bilal.ccmm.app;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.bilal.ccmm.app.settings.Settings;
import dev.bilal.ccmm.app.settings.SettingsStore;
import dev.bilal.ccmm.app.workbench.DirectoryPrompt;
import dev.bilal.ccmm.app.workbench.Theme;
import dev.bilal.ccmm.app.workbench.Workbench;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ThemeTest {
    @TempDir
    Path configDir;

    @Test
    void workbenchDefaultsToThemeDarkStyleClass() {
        Workbench workbench = new Workbench();
        assertTrue(workbench.root().getStyleClass().contains("theme-dark"));
        assertFalse(workbench.root().getStyleClass().contains("theme-light"));
        assertTrue(workbench.theme() == Theme.DARK);
    }

    @Test
    void applyThemeLightSwapsStyleClass() {
        Workbench workbench = new Workbench();
        workbench.applyTheme(Theme.LIGHT);
        assertTrue(workbench.root().getStyleClass().contains("theme-light"));
        assertFalse(workbench.root().getStyleClass().contains("theme-dark"));
        assertTrue(workbench.theme() == Theme.LIGHT);
    }

    @Test
    void savedLightSettingsApplyThemeLightOnIdeWindow() {
        SettingsStore store = new SettingsStore(configDir);
        store.setTheme(Settings.Theme.LIGHT);
        IdeWindow window = new IdeWindow(40, DirectoryPrompt.fixed(configDir), store);
        assertTrue(window.workbench().root().getStyleClass().contains("theme-light"));
        assertFalse(window.workbench().root().getStyleClass().contains("theme-dark"));
    }

    @Test
    void noHexColourLiteralsInMainJava() throws Exception {
        Pattern hexColour = Pattern.compile("#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?\\b");
        Path main = Path.of("src/main/java");
        if (!Files.isDirectory(main)) {
            main = Path.of("ccmm-app/src/main/java");
        }
        try (Stream<Path> paths = Files.walk(main)) {
            paths.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
                try {
                    String source = Files.readString(path, StandardCharsets.UTF_8);
                    assertFalse(hexColour.matcher(source).find(), () -> "hex colour in " + path);
                } catch (Exception failure) {
                    throw new AssertionError(path + ": " + failure.getMessage(), failure);
                }
            });
        }
    }
}
