package dev.bilal.ccmm.app.settings;

import java.util.Objects;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Side-bar Settings fields bound to {@link SettingsStore}. */
public final class SettingsPanel {
    public static final String RESET_PROMPT = "Reset all settings?";

    private final SettingsStore store;
    private final ConfirmPrompt confirm;
    private final VBox root = new VBox(8);
    private final ComboBox<String> theme = new ComboBox<>();
    private final Spinner<Integer> fontSize =
            new Spinner<>(Settings.MIN_FONT_SIZE, Settings.MAX_FONT_SIZE, Settings.DEFAULT_FONT_SIZE);
    private final ComboBox<String> ligatures = new ComboBox<>();
    private final TextField relayUrl = new TextField();
    private final Label relayError = new Label();
    private final TextField displayName = new TextField();
    private final Button reset = new Button("Reset to defaults");
    private boolean syncing;

    public SettingsPanel(SettingsStore store, ConfirmPrompt confirm) {
        this.store = Objects.requireNonNull(store);
        this.confirm = Objects.requireNonNull(confirm);
        root.setId("settings-panel");
        root.setPadding(new Insets(8));
        theme.setId("settings-theme");
        theme.getItems().setAll("Dark", "Light");
        fontSize.setId("settings-font-size");
        fontSize.setEditable(true);
        ligatures.setId("settings-ligatures");
        ligatures.getItems().setAll("Off", "On");
        relayUrl.setId("settings-relay-url");
        relayError.setId("settings-relay-error");
        relayError.setVisible(false);
        relayError.setManaged(false);
        displayName.setId("settings-display-name");
        reset.setId("settings-reset");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);
        ColumnConstraints labels = new ColumnConstraints();
        labels.setMinWidth(110);
        ColumnConstraints fields = new ColumnConstraints();
        fields.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labels, fields);
        int row = 0;
        grid.add(new Label("Theme"), 0, row);
        grid.add(theme, 1, row++);
        grid.add(new Label("Editor font size"), 0, row);
        grid.add(fontSize, 1, row++);
        grid.add(new Label("Ligatures"), 0, row);
        grid.add(ligatures, 1, row++);
        grid.add(new Label("Relay URL"), 0, row);
        grid.add(relayUrl, 1, row++);
        grid.add(relayError, 1, row++);
        grid.add(new Label("Display name"), 0, row);
        grid.add(displayName, 1, row);

        root.getChildren().addAll(grid, reset);
        theme.valueProperty().addListener((obs, previous, value) -> {
            if (!syncing && value != null) {
                store.setTheme("Light".equals(value) ? Settings.Theme.LIGHT : Settings.Theme.DARK);
            }
        });
        fontSize.valueProperty().addListener((obs, previous, value) -> {
            if (!syncing && value != null) {
                store.setEditorFontSize(value);
            }
        });
        ligatures.valueProperty().addListener((obs, previous, value) -> {
            if (!syncing && value != null) {
                store.setLigatures("On".equals(value) ? Settings.Ligatures.ON : Settings.Ligatures.OFF);
            }
        });
        relayUrl.focusedProperty().addListener((obs, was, focused) -> {
            if (was && !focused) {
                commitRelay();
            }
        });
        relayUrl.setOnAction(event -> commitRelay());
        displayName.focusedProperty().addListener((obs, was, focused) -> {
            if (was && !focused) {
                store.setDisplayName(displayName.getText());
                syncFromStore();
            }
        });
        displayName.setOnAction(event -> {
            store.setDisplayName(displayName.getText());
            syncFromStore();
        });
        reset.setOnAction(event -> {
            if (confirm.confirm(RESET_PROMPT, root.getScene() == null ? null : root.getScene().getWindow())) {
                store.resetToDefaults();
                syncFromStore();
            }
        });
        syncFromStore();
    }

    public Node root() {
        return root;
    }

    public void syncFromStore() {
        syncing = true;
        try {
            Settings settings = store.current();
            theme.setValue(settings.theme().label());
            fontSize.getValueFactory().setValue(settings.editorFontSize());
            ligatures.setValue(settings.ligatures().label());
            relayUrl.setText(settings.relayUrl());
            displayName.setText(settings.displayName());
            showRelayError(null);
        } finally {
            syncing = false;
        }
    }

    private void commitRelay() {
        String error = store.setRelayUrl(relayUrl.getText());
        showRelayError(error);
        if (error != null) {
            relayUrl.setText(store.current().relayUrl());
        }
    }

    private void showRelayError(String message) {
        boolean show = message != null && !message.isBlank();
        relayError.setText(show ? message : "");
        relayError.setVisible(show);
        relayError.setManaged(show);
    }
}
