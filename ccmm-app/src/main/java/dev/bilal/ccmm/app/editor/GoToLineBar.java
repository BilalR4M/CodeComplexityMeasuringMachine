package dev.bilal.ccmm.app.editor;

import java.util.Objects;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Small go-to-line field over the editor. */
public final class GoToLineBar {
    public static final String TITLE = "Go to Line…";

    private final VBox root = new VBox(4);
    private final TextField field = new TextField();
    private final Button go = new Button("Go");
    private final Button close = new Button("×");
    private Consumer<String> onGo = line -> {};
    private Runnable onClose = () -> {};

    public GoToLineBar() {
        root.setId("go-to-line-bar");
        root.setPadding(new Insets(4, 8, 4, 8));
        root.setVisible(false);
        root.setManaged(false);

        field.setId("go-to-line-field");
        field.setPromptText(TITLE);
        field.setAccessibleText(TITLE);
        go.setId("go-to-line-go");
        go.setTooltip(new Tooltip(TITLE));
        go.setAccessibleText(TITLE);
        close.setId("go-to-line-close");
        close.setTooltip(new Tooltip("Close"));
        close.setAccessibleText("Close");

        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        Label label = new Label(TITLE);
        HBox.setHgrow(field, Priority.ALWAYS);
        row.getChildren().addAll(label, field, go, close);
        root.getChildren().add(row);

        field.setOnAction(event -> submit());
        go.setOnAction(event -> submit());
        close.setOnAction(event -> onClose.run());
        field.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                onClose.run();
                event.consume();
            }
        });
    }

    public VBox root() {
        return root;
    }

    public TextField field() {
        return field;
    }

    public void setOnGo(Consumer<String> onGo) {
        this.onGo = Objects.requireNonNull(onGo);
    }

    public void setOnClose(Runnable onClose) {
        this.onClose = Objects.requireNonNull(onClose);
    }

    public void show() {
        root.setVisible(true);
        root.setManaged(true);
        field.requestFocus();
        field.selectAll();
    }

    public void hide() {
        root.setVisible(false);
        root.setManaged(false);
    }

    public boolean isShowing() {
        return root.isVisible();
    }

    private void submit() {
        onGo.accept(field.getText() == null ? "" : field.getText());
    }
}
