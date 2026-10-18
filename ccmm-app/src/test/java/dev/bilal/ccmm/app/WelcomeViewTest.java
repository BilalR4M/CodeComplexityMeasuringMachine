package dev.bilal.ccmm.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.bilal.ccmm.app.session.RecentEntry;
import dev.bilal.ccmm.app.session.RecentStore;
import dev.bilal.ccmm.app.session.SessionStore;
import dev.bilal.ccmm.app.settings.SettingsStore;
import dev.bilal.ccmm.app.welcome.WelcomeView;
import dev.bilal.ccmm.app.workbench.CommandId;
import dev.bilal.ccmm.app.workbench.CommandRegistry;
import dev.bilal.ccmm.app.workbench.DirectoryPrompt;
import dev.bilal.ccmm.app.workbench.Keymap;
import dev.bilal.ccmm.app.workbench.SimpleCommand;
import java.awt.GraphicsEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.scene.control.Hyperlink;
import javafx.stage.Stage;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WelcomeViewTest {
    @TempDir
    Path configDir;

    @TempDir
    Path projectDir;

    @Test
    void startSessionDisabledUntilSessionStartRegistered() {
        WelcomeView view = new WelcomeView(Keymap.seeded(Keymap.Platform.OTHER));
        CommandRegistry empty = new CommandRegistry();
        view.refresh(List.of(), empty);
        assertTrue(view.startSessionButton().isDisable());
        assertEquals(WelcomeView.SESSIONS_LATER, view.startSessionButton().getTooltip().getText());

        CommandRegistry withSession = new CommandRegistry();
        withSession.register(new SimpleCommand(CommandId.SESSION_START, "Start a Session", wb -> {}));
        view.refresh(List.of(), withSession);
        assertFalse(view.startSessionButton().isDisable());
    }

    @Test
    void missingRecentClickReportsFolderMissingWithoutThrowing() {
        WelcomeView view = new WelcomeView(Keymap.seeded(Keymap.Platform.OTHER));
        AtomicReference<RecentEntry> clicked = new AtomicReference<>();
        view.setOnRecent(clicked::set);
        Path gone = projectDir.resolve("deleted-project");
        RecentEntry missing = new RecentEntry(gone, true);
        view.refresh(List.of(missing), new CommandRegistry());
        assertEquals(1, view.recentBox().getChildren().size());
        Hyperlink link = (Hyperlink) view.recentBox().getChildren().get(0);
        assertEquals(WelcomeView.FOLDER_MISSING, link.getText());
        link.fire();
        assertEquals(missing, clicked.get());
    }

    @Test
    void shortcutLinesUseCtrlOnWindows() {
        WelcomeView view = new WelcomeView(Keymap.seeded(Keymap.Platform.OTHER));
        view.refresh(List.of(), new CommandRegistry());
        String shortcuts = view.root().lookupAll(".welcome-shortcut").stream()
                .map(node -> ((javafx.scene.control.Label) node).getText())
                .reduce("", (a, b) -> a + "\n" + b);
        assertTrue(shortcuts.contains("Ctrl+P Quick open"));
        assertTrue(shortcuts.contains("Ctrl+Shift+P Commands"));
        assertTrue(shortcuts.contains("Ctrl+Shift+F Search"));
    }

    @Test
    void noFolderStartupShowsOpenFolder() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        SettingsStore settings = new SettingsStore(configDir);
        SessionStore sessions = new SessionStore(configDir);

        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Runnable start = () -> {
            try {
                Platform.setImplicitExit(false);
                IdeWindow window = new IdeWindow(40, DirectoryPrompt.fixed(projectDir), settings, sessions);
                Stage stage = new Stage();
                stage.setScene(window.scene());
                stage.show();
                assertTrue(window.isWelcome());
                assertTrue(window.isShowingWelcome());
                assertEquals(WelcomeView.OPEN_FOLDER, window.welcomeView().openFolderButton().getText());
                assertTrue(window.welcomeView().openFolderButton().isVisible());
                assertEquals(
                        WelcomeView.TITLE,
                        window.workbench().commands().find(CommandId.VIEW_WELCOME.value()).orElseThrow().title());
                stage.close();
                window.close();
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
    void missingRecentSetsStatusFolderMissing() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        Path gone = projectDir.resolve("no-longer-here");
        RecentStore recents = new RecentStore(configDir);
        Files.createDirectories(configDir);
        Files.writeString(recents.projectsFile(), gone.toAbsolutePath().normalize() + "\n");
        SettingsStore settings = new SettingsStore(configDir);
        SessionStore sessions = new SessionStore(configDir);

        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Runnable start = () -> {
            try {
                Platform.setImplicitExit(false);
                IdeWindow window = new IdeWindow(40, DirectoryPrompt.fixed(projectDir), settings, sessions);
                Stage stage = new Stage();
                stage.setScene(window.scene());
                stage.show();
                window.workbench().execute(CommandId.VIEW_WELCOME.value());
                assertTrue(window.isShowingWelcome());
                Hyperlink link = (Hyperlink) window.welcomeView().recentBox().getChildren().get(0);
                link.fire();
                assertEquals(WelcomeView.FOLDER_MISSING, window.workbench().statusBar().leftText());
                assertTrue(window.isShowingWelcome());
                stage.close();
                window.close();
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
