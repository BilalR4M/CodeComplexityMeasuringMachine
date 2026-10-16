package dev.bilal.ccmm.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.bilal.ccmm.app.settings.ConfirmPrompt;
import dev.bilal.ccmm.app.settings.Settings;
import dev.bilal.ccmm.app.settings.SettingsPanel;
import dev.bilal.ccmm.app.settings.SettingsStore;
import java.awt.GraphicsEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SettingsStoreTest {
    @TempDir
    Path configDir;

    @Test
    void missingFileLoadsDefaults() {
        SettingsStore store = new SettingsStore(configDir);
        Settings settings = store.load();
        assertEquals(Settings.Theme.DARK, settings.theme());
        assertEquals(Settings.DEFAULT_FONT_SIZE, settings.editorFontSize());
        assertEquals(Settings.Ligatures.OFF, settings.ligatures());
        assertEquals("", settings.relayUrl());
        assertEquals(System.getProperty("user.name", "User"), settings.displayName());
    }

    @Test
    void savedLightThemeReloadsAsLight() {
        SettingsStore writer = new SettingsStore(configDir);
        writer.setTheme(Settings.Theme.LIGHT);
        SettingsStore reader = new SettingsStore(configDir);
        assertEquals(Settings.Theme.LIGHT, reader.load().theme());
    }

    @Test
    void invalidRelayUrlKeepsPreviousValue() {
        SettingsStore store = new SettingsStore(configDir);
        assertNull(store.setRelayUrl("wss://relay.example"));
        assertEquals("wss://relay.example", store.current().relayUrl());

        assertEquals(SettingsStore.RELAY_ERROR, store.setRelayUrl("http://example"));
        assertEquals("wss://relay.example", store.current().relayUrl());

        SettingsStore reloaded = new SettingsStore(configDir);
        assertEquals("wss://relay.example", reloaded.load().relayUrl());
    }

    @Test
    void fontSizeOutsideRangeDoesNotSave() {
        SettingsStore store = new SettingsStore(configDir);
        assertTrue(store.setEditorFontSize(16));
        assertFalse(store.setEditorFontSize(7));
        assertFalse(store.setEditorFontSize(33));
        assertEquals(16, store.current().editorFontSize());
        assertEquals(16, new SettingsStore(configDir).load().editorFontSize());
    }

    @Test
    void resetConfirmedWritesDefaultsToDisk() throws Exception {
        SettingsStore store = new SettingsStore(configDir);
        store.setTheme(Settings.Theme.LIGHT);
        store.setEditorFontSize(20);
        store.setLigatures(Settings.Ligatures.ON);
        store.setRelayUrl("ws://localhost:9000");
        store.setDisplayName("Ada");
        assertTrue(Files.isRegularFile(store.file()));

        store.resetToDefaults();
        Settings defaults = new SettingsStore(configDir).load();
        assertEquals(Settings.Theme.DARK, defaults.theme());
        assertEquals(Settings.DEFAULT_FONT_SIZE, defaults.editorFontSize());
        assertEquals(Settings.Ligatures.OFF, defaults.ligatures());
        assertEquals("", defaults.relayUrl());
        assertEquals(System.getProperty("user.name", "User"), defaults.displayName());
    }

    @Test
    void settingsPanelShowsRelayErrorAndKeepsPreviousUrl() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        SettingsStore store = new SettingsStore(configDir);
        store.setRelayUrl("wss://good.example");

        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Runnable start = () -> {
            try {
                Platform.setImplicitExit(false);
                SettingsPanel panel = new SettingsPanel(store, ConfirmPrompt.always(true));
                TextField relay = (TextField) panel.root().lookup("#settings-relay-url");
                Label error = (Label) panel.root().lookup("#settings-relay-error");
                relay.setText("http://example");
                relay.getOnAction().handle(null);
                assertEquals(SettingsStore.RELAY_ERROR, error.getText());
                assertTrue(error.isVisible());
                assertEquals("wss://good.example", store.current().relayUrl());
                assertEquals("wss://good.example", relay.getText());
                done.countDown();
            } catch (Throwable thrown) {
                failure.set(thrown);
                done.countDown();
            }
        };
        try {
            Platform.startup(start);
        } catch (IllegalStateException alreadyRunning) {
            Platform.runLater(start);
        }
        assertTrue(done.await(15, TimeUnit.SECONDS));
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
    }

    @Test
    void settingsPanelResetConfirmedRestoresDefaults() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        SettingsStore store = new SettingsStore(configDir);
        store.setTheme(Settings.Theme.LIGHT);
        store.setDisplayName("Ada");

        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Runnable start = () -> {
            try {
                Platform.setImplicitExit(false);
                SettingsPanel panel = new SettingsPanel(store, ConfirmPrompt.always(true));
                ((Button) panel.root().lookup("#settings-reset")).fire();
                assertEquals(Settings.Theme.DARK, store.current().theme());
                assertEquals(System.getProperty("user.name", "User"), store.current().displayName());
                done.countDown();
            } catch (Throwable thrown) {
                failure.set(thrown);
                done.countDown();
            }
        };
        try {
            Platform.startup(start);
        } catch (IllegalStateException alreadyRunning) {
            Platform.runLater(start);
        }
        assertTrue(done.await(15, TimeUnit.SECONDS));
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
    }
}
