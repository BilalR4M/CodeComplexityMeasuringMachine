package dev.bilal.ccmm.app.workbench;

import dev.bilal.ccmm.app.document.LineEnding;
import dev.bilal.ccmm.app.document.Utf8Text;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * Status bar: left slot (git / messages, EOL, encoding) and right slot (caret).
 * Height 24. FX thread only for updates.
 */
public final class StatusBar extends BorderPane {
    public static final String NO_GIT = "no git";
    public static final String COULD_NOT_OPEN = "Could not open folder";

    private final Label left = new Label(NO_GIT);
    private final MenuButton eol = new MenuButton(LineEnding.LF.label());
    private final Label encoding = new Label(Utf8Text.ENCODING_LABEL);
    private final Label right = new Label("Ln 1, Col 1");
    private Consumer<LineEnding> onLineEndingChosen = ending -> {};

    public StatusBar() {
        setId("status-bar");
        setMinHeight(24);
        setMaxHeight(24);
        setPrefHeight(24);
        setPadding(new Insets(0, 8, 0, 8));
        left.setId("status-left");
        right.setId("status-right");
        left.setAccessibleText("Status");
        right.setAccessibleText("Caret position");

        eol.setId("status-eol");
        eol.setAccessibleText("Line ending");
        MenuItem lf = new MenuItem(LineEnding.LF.label());
        lf.setId("status-eol-lf");
        lf.setOnAction(event -> choose(LineEnding.LF));
        MenuItem crlf = new MenuItem(LineEnding.CRLF.label());
        crlf.setId("status-eol-crlf");
        crlf.setOnAction(event -> choose(LineEnding.CRLF));
        eol.getItems().addAll(lf, crlf);

        encoding.setId("status-encoding");
        encoding.setAccessibleText("Encoding");
        encoding.setText(Utf8Text.ENCODING_LABEL);

        HBox leftBox = new HBox(12, left, eol, encoding);
        leftBox.setAlignment(Pos.CENTER_LEFT);
        HBox rightBox = new HBox(right);
        rightBox.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(leftBox, Priority.ALWAYS);
        setLeft(leftBox);
        setRight(rightBox);
        setCaret(1, 1);
        clearFileEncoding();
    }

    public void setLeftMessage(String text) {
        left.setText(text == null || text.isBlank() ? NO_GIT : text);
    }

    public void clearMessage() {
        left.setText(NO_GIT);
    }

    public void setCaret(int line, int column) {
        int safeLine = Math.max(1, line);
        int safeColumn = Math.max(1, column);
        right.setText("Ln " + safeLine + ", Col " + safeColumn);
    }

    public void setOnLineEndingChosen(Consumer<LineEnding> onLineEndingChosen) {
        this.onLineEndingChosen = Objects.requireNonNull(onLineEndingChosen);
    }

    public void showFileEncoding(LineEnding ending, String encodingName) {
        LineEnding shown = ending == null ? LineEnding.LF : ending;
        eol.setText(shown.label());
        eol.setVisible(true);
        eol.setManaged(true);
        encoding.setText(encodingName == null || encodingName.isBlank() ? Utf8Text.ENCODING_LABEL : encodingName);
        encoding.setVisible(true);
        encoding.setManaged(true);
    }

    public void clearFileEncoding() {
        eol.setVisible(false);
        eol.setManaged(false);
        encoding.setVisible(false);
        encoding.setManaged(false);
    }

    public String leftText() {
        return left.getText();
    }

    public String rightText() {
        return right.getText();
    }

    public String eolText() {
        return eol.getText();
    }

    public String encodingText() {
        return encoding.getText();
    }

    public MenuButton eolButton() {
        return eol;
    }

    private void choose(LineEnding ending) {
        eol.setText(ending.label());
        onLineEndingChosen.accept(ending);
    }
}
