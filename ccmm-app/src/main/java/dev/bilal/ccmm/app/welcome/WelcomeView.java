package dev.bilal.ccmm.app.welcome;

import dev.bilal.ccmm.app.session.RecentEntry;
import dev.bilal.ccmm.app.workbench.CommandId;
import dev.bilal.ccmm.app.workbench.CommandRegistry;
import dev.bilal.ccmm.app.workbench.Keymap;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Editor-region welcome when no folder is open (and via Show Welcome).
 * FX thread only. Tokens via CSS; no hex in Java.
 */
public final class WelcomeView {
    public static final String HEADING = "CCMM IDE";
    public static final String OPEN_FOLDER = "Open Folder…";
    public static final String START_SESSION = "Start a Session…";
    public static final String RECENT = "Recent";
    public static final String FOLDER_MISSING = "Folder missing";
    public static final String SESSIONS_LATER = "Sessions arrive in a later change";
    public static final String TITLE = "Show Welcome";
    public static final String QUICK_OPEN_LABEL = "Quick open";
    public static final String COMMANDS_LABEL = "Commands";
    public static final String SEARCH_LABEL = "Search";

    private final Keymap keymap;
    private final VBox root = new VBox(16);
    private final Button openFolder = new Button(OPEN_FOLDER);
    private final Button startSession = new Button(START_SESSION);
    private final VBox recentBox = new VBox(4);
    private final Label recentHeading = new Label(RECENT);
    private final VBox shortcutBox = new VBox(4);
    private Runnable onOpenFolder = () -> {};
    private Consumer<RecentEntry> onRecent = entry -> {};
    private Runnable onStartSession = () -> {};

    public WelcomeView(Keymap keymap) {
        this.keymap = Objects.requireNonNull(keymap);
        root.setId("welcome-view");
        root.getStyleClass().add("welcome-view");
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(48));
        root.setFillWidth(false);

        Label heading = new Label(HEADING);
        heading.setId("welcome-heading");
        heading.getStyleClass().add("welcome-heading");
        heading.setAccessibleText(HEADING);

        openFolder.setId("welcome-open-folder");
        openFolder.setAccessibleText(OPEN_FOLDER);
        openFolder.setOnAction(event -> onOpenFolder.run());

        startSession.setId("welcome-start-session");
        startSession.setAccessibleText(START_SESSION);
        startSession.setOnAction(event -> onStartSession.run());

        HBox actions = new HBox(12, openFolder, startSession);
        actions.setId("welcome-actions");
        actions.setAlignment(Pos.CENTER);

        recentHeading.setId("welcome-recent-heading");
        recentHeading.getStyleClass().add("welcome-section");
        recentBox.setId("welcome-recent");
        recentBox.setAlignment(Pos.CENTER_LEFT);

        shortcutBox.setId("welcome-shortcuts");
        shortcutBox.setAlignment(Pos.CENTER_LEFT);
        rebuildShortcuts();

        VBox body = new VBox(16, heading, actions, recentHeading, recentBox, shortcutBox);
        body.setAlignment(Pos.CENTER_LEFT);
        body.setMaxWidth(420);
        root.getChildren().add(body);
        refresh(List.of(), new CommandRegistry());
    }

    public Node root() {
        return root;
    }

    public Button openFolderButton() {
        return openFolder;
    }

    public Button startSessionButton() {
        return startSession;
    }

    public VBox recentBox() {
        return recentBox;
    }

    public void setOnOpenFolder(Runnable onOpenFolder) {
        this.onOpenFolder = Objects.requireNonNull(onOpenFolder);
    }

    public void setOnRecent(Consumer<RecentEntry> onRecent) {
        this.onRecent = Objects.requireNonNull(onRecent);
    }

    public void setOnStartSession(Runnable onStartSession) {
        this.onStartSession = Objects.requireNonNull(onStartSession);
    }

    /** Rebuilds recents and enables Start a Session only when {@code session.start} is registered. */
    public void refresh(List<RecentEntry> projects, CommandRegistry commands) {
        Objects.requireNonNull(projects);
        Objects.requireNonNull(commands);
        boolean sessionReady = commands.find(CommandId.SESSION_START.value()).isPresent();
        startSession.setDisable(!sessionReady);
        if (sessionReady) {
            startSession.setTooltip(null);
        } else {
            startSession.setTooltip(new Tooltip(SESSIONS_LATER));
        }

        recentBox.getChildren().clear();
        boolean hasRecents = !projects.isEmpty();
        recentHeading.setVisible(hasRecents);
        recentHeading.setManaged(hasRecents);
        for (RecentEntry entry : projects) {
            recentBox.getChildren().add(recentRow(entry));
        }
        rebuildShortcuts();
    }

    private Node recentRow(RecentEntry entry) {
        String name = displayName(entry.path());
        Hyperlink link = new Hyperlink(entry.missing() ? FOLDER_MISSING : name);
        link.getStyleClass().add("welcome-recent-item");
        if (entry.missing()) {
            link.getStyleClass().add("band-warn");
            link.setAccessibleText(FOLDER_MISSING);
        } else {
            link.setAccessibleText(name);
        }
        link.setOnAction(event -> onRecent.accept(entry));
        return link;
    }

    private void rebuildShortcuts() {
        shortcutBox.getChildren().clear();
        shortcutBox
                .getChildren()
                .addAll(
                        shortcutLine(CommandId.QUICK_OPEN, QUICK_OPEN_LABEL),
                        shortcutLine(CommandId.COMMAND_PALETTE, COMMANDS_LABEL),
                        shortcutLine(CommandId.VIEW_SEARCH, SEARCH_LABEL));
    }

    private Label shortcutLine(CommandId id, String action) {
        String binding = keymap.shortcutLabel(id.value());
        String text = binding.isBlank() ? action : binding + " " + action;
        Label line = new Label(text);
        line.getStyleClass().addAll("welcome-shortcut", "text-secondary");
        line.setAccessibleText(text);
        return line;
    }

    static String displayName(Path path) {
        if (path == null) {
            return "";
        }
        Path name = path.getFileName();
        return name == null ? path.toString() : name.toString();
    }
}
