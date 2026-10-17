package dev.bilal.ccmm.app.editor;

import java.util.Objects;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Find / replace chrome over the editor. */
public final class FindReplaceBar {
    public static final String FIND_NAME = "Find";
    public static final String REPLACE_NAME = "Replace";
    public static final String NO_MATCHES = "No matches";

    private final VBox root = new VBox(4);
    private final TextField findField = new TextField();
    private final TextField replaceField = new TextField();
    private final CheckBox matchCase = new CheckBox("Match case");
    private final Button next = new Button("Next");
    private final Button previous = new Button("Previous");
    private final Button replace = new Button("Replace");
    private final Button replaceAll = new Button("Replace All");
    private final Button close = new Button("×");
    private final HBox replaceRow = new HBox(8);
    private boolean replaceMode;
    private Runnable onNext = () -> {};
    private Runnable onPrevious = () -> {};
    private Runnable onReplace = () -> {};
    private Runnable onReplaceAll = () -> {};
    private Runnable onClose = () -> {};
    private Consumer<String> onQueryChanged = query -> {};

    public FindReplaceBar() {
        root.setId("find-replace-bar");
        root.setPadding(new Insets(4, 8, 4, 8));
        root.setVisible(false);
        root.setManaged(false);

        findField.setId("find-field");
        findField.setPromptText(FIND_NAME);
        findField.setAccessibleText(FIND_NAME);
        replaceField.setId("replace-field");
        replaceField.setPromptText(REPLACE_NAME);
        replaceField.setAccessibleText(REPLACE_NAME);
        matchCase.setId("find-match-case");
        next.setId("find-next");
        next.setTooltip(new Tooltip("Find Next"));
        next.setAccessibleText("Find Next");
        previous.setId("find-previous");
        previous.setTooltip(new Tooltip("Find Previous"));
        previous.setAccessibleText("Find Previous");
        replace.setId("find-replace");
        replace.setTooltip(new Tooltip("Replace"));
        replace.setAccessibleText("Replace");
        replaceAll.setId("find-replace-all");
        replaceAll.setTooltip(new Tooltip("Replace All"));
        replaceAll.setAccessibleText("Replace All");
        close.setId("find-close");
        close.setTooltip(new Tooltip("Close"));
        close.setAccessibleText("Close");

        HBox findRow = new HBox(8);
        findRow.setAlignment(Pos.CENTER_LEFT);
        Label findLabel = new Label(FIND_NAME);
        HBox.setHgrow(findField, Priority.ALWAYS);
        findRow.getChildren().addAll(findLabel, findField, matchCase, previous, next, close);

        replaceRow.setAlignment(Pos.CENTER_LEFT);
        Label replaceLabel = new Label(REPLACE_NAME);
        HBox.setHgrow(replaceField, Priority.ALWAYS);
        replaceRow.getChildren().addAll(replaceLabel, replaceField, replace, replaceAll);
        replaceRow.setVisible(false);
        replaceRow.setManaged(false);

        root.getChildren().addAll(findRow, replaceRow);

        findField.textProperty().addListener((obs, previousText, text) -> onQueryChanged.accept(text == null ? "" : text));
        findField.setOnAction(event -> onNext.run());
        next.setOnAction(event -> onNext.run());
        previous.setOnAction(event -> onPrevious.run());
        replace.setOnAction(event -> onReplace.run());
        replaceAll.setOnAction(event -> onReplaceAll.run());
        close.setOnAction(event -> onClose.run());
        matchCase.selectedProperty().addListener((obs, was, selected) -> onQueryChanged.accept(findField.getText()));
    }

    public VBox root() {
        return root;
    }

    public TextField findField() {
        return findField;
    }

    public TextField replaceField() {
        return replaceField;
    }

    public boolean matchCase() {
        return matchCase.isSelected();
    }

    public boolean replaceMode() {
        return replaceMode;
    }

    public void setOnNext(Runnable onNext) {
        this.onNext = Objects.requireNonNull(onNext);
    }

    public void setOnPrevious(Runnable onPrevious) {
        this.onPrevious = Objects.requireNonNull(onPrevious);
    }

    public void setOnReplace(Runnable onReplace) {
        this.onReplace = Objects.requireNonNull(onReplace);
    }

    public void setOnReplaceAll(Runnable onReplaceAll) {
        this.onReplaceAll = Objects.requireNonNull(onReplaceAll);
    }

    public void setOnClose(Runnable onClose) {
        this.onClose = Objects.requireNonNull(onClose);
    }

    public void setOnQueryChanged(Consumer<String> onQueryChanged) {
        this.onQueryChanged = Objects.requireNonNull(onQueryChanged);
    }

    public void show(boolean replace) {
        this.replaceMode = replace;
        replaceRow.setVisible(replace);
        replaceRow.setManaged(replace);
        root.setVisible(true);
        root.setManaged(true);
        findField.requestFocus();
        findField.selectAll();
    }

    public void hide() {
        root.setVisible(false);
        root.setManaged(false);
    }

    public boolean isShowing() {
        return root.isVisible();
    }
}
