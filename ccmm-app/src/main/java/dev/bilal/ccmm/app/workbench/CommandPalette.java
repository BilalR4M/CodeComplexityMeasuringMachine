package dev.bilal.ccmm.app.workbench;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Window;

/** Ctrl/Cmd+Shift+P popup: filter registered commands and run one. */
public final class CommandPalette {
    public static final String PLACEHOLDER = "Type a command";
    public static final String NO_MATCH = "No matching commands";
    public static final String TITLE = "Command Palette";
    public static final int RECENT_LIMIT = 10;

    private final Popup popup = new Popup();
    private final VBox root = new VBox(6);
    private final TextField field = new TextField();
    private final ListView<Command> list = new ListView<>();
    private final Label empty = new Label(NO_MATCH);
    private final Deque<String> recent = new ArrayDeque<>();
    private final Keymap keymap;
    private CommandRegistry registry = new CommandRegistry();
    private Consumer<String> onRun = id -> {};

    public CommandPalette(Keymap keymap) {
        this.keymap = Objects.requireNonNull(keymap);
        root.setId("command-palette");
        root.setPadding(new Insets(10));
        root.setPrefWidth(480);
        root.setMaxHeight(360);
        root.getStyleClass().add("command-palette");

        field.setId("command-palette-field");
        field.setPromptText(PLACEHOLDER);
        field.setAccessibleText(PLACEHOLDER);
        list.setId("command-palette-list");
        list.setPrefHeight(280);
        empty.setId("command-palette-empty");
        empty.setVisible(false);
        empty.setManaged(false);

        list.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(Command command, boolean emptyRow) {
                super.updateItem(command, emptyRow);
                if (emptyRow || command == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label title = new Label(command.title());
                Label shortcut = new Label(keymap.shortcutLabel(command.id()));
                shortcut.getStyleClass().add("band-warn");
                HBox row = new HBox(12);
                row.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(title, Priority.ALWAYS);
                row.getChildren().addAll(title, shortcut);
                setGraphic(row);
                setText(null);
            }
        });

        root.getChildren().addAll(field, list, empty);
        popup.getContent().add(root);
        popup.setAutoHide(true);

        field.textProperty().addListener((obs, previous, text) -> applyFilter());
        field.setOnAction(event -> runSelected());
        field.addEventFilter(KeyEvent.KEY_PRESSED, this::onFieldKey);
        list.setOnMouseClicked(event -> {
            if (event.getClickCount() >= 2) {
                runSelected();
            }
        });
    }

    public Popup popup() {
        return popup;
    }

    public TextField field() {
        return field;
    }

    public ListView<Command> list() {
        return list;
    }

    public Label emptyLabel() {
        return empty;
    }

    public List<String> recentIds() {
        return List.copyOf(recent);
    }

    public void setOnRun(Consumer<String> onRun) {
        this.onRun = Objects.requireNonNull(onRun);
    }

    public boolean isShowing() {
        return popup.isShowing();
    }

    public void show(Window owner, CommandRegistry commands) {
        this.registry = Objects.requireNonNull(commands);
        field.clear();
        applyFilter();
        if (owner == null) {
            return;
        }
        double x = owner.getX() + Math.max(40, (owner.getWidth() - root.getPrefWidth()) / 2);
        double y = owner.getY() + 80;
        popup.show(owner, x, y);
        field.requestFocus();
    }

    public void hide() {
        popup.hide();
    }

    public void remember(String commandId) {
        if (commandId == null || commandId.isBlank()) {
            return;
        }
        recent.remove(commandId);
        recent.addFirst(commandId);
        while (recent.size() > RECENT_LIMIT) {
            recent.removeLast();
        }
    }

    private void applyFilter() {
        List<Command> rows = registry.filter(field.getText(), new ArrayList<>(recent));
        list.getItems().setAll(rows);
        boolean none = rows.isEmpty();
        empty.setVisible(none);
        empty.setManaged(none);
        list.setVisible(!none);
        list.setManaged(!none);
        if (!none) {
            list.getSelectionModel().select(0);
        }
    }

    private void onFieldKey(KeyEvent event) {
        if (event.getCode() == KeyCode.ESCAPE) {
            hide();
            event.consume();
            return;
        }
        if (event.getCode() == KeyCode.DOWN) {
            list.getSelectionModel().selectNext();
            event.consume();
            return;
        }
        if (event.getCode() == KeyCode.UP) {
            list.getSelectionModel().selectPrevious();
            event.consume();
        }
    }

    private void runSelected() {
        Command selected = list.getSelectionModel().getSelectedItem();
        if (selected == null && !list.getItems().isEmpty()) {
            selected = list.getItems().get(0);
        }
        if (selected == null) {
            return;
        }
        String id = selected.id();
        remember(id);
        hide();
        onRun.accept(id);
    }
}
