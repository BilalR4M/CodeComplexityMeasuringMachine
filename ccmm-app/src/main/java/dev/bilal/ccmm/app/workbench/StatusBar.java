package dev.bilal.ccmm.app.workbench;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * Status bar: left slot (git / messages) and right slot (caret).
 * Height 24. FX thread only for updates.
 */
public final class StatusBar extends BorderPane {
    public static final String NO_GIT = "no git";
    public static final String COULD_NOT_OPEN = "Could not open folder";

    private final Label left = new Label(NO_GIT);
    private final Label right = new Label("Ln 1, Col 1");

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
        HBox leftBox = new HBox(left);
        leftBox.setAlignment(Pos.CENTER_LEFT);
        HBox rightBox = new HBox(right);
        rightBox.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(leftBox, Priority.ALWAYS);
        setLeft(leftBox);
        setRight(rightBox);
        setCaret(1, 1);
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

    public String leftText() {
        return left.getText();
    }

    public String rightText() {
        return right.getText();
    }
}
