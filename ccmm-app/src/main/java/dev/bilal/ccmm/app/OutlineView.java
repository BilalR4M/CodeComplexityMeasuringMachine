package dev.bilal.ccmm.app;

import dev.bilal.ccmm.core.MethodMetrics;
import java.util.List;
import java.util.Objects;
import java.util.function.IntConsumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Side-bar outline: method names and start lines from the last analysis. */
public final class OutlineView {
    public static final String NO_DOCUMENT = "Open a file to see its outline";
    public static final String NO_OUTLINE = "No outline for this file";
    public static final String ANALYZING = "Analyzing…";

    private final VBox root = new VBox(4);
    private final Label message = new Label(NO_DOCUMENT);
    private final ListView<MethodMetrics> list = new ListView<>();
    private IntConsumer onGoToLine = line -> {};

    public OutlineView() {
        root.setId("outline-view");
        root.setPadding(new Insets(8));
        message.setId("outline-message");
        message.setWrapText(true);
        message.setAccessibleText(NO_DOCUMENT);
        list.setId("outline-list");
        list.setVisible(false);
        list.setManaged(false);
        list.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(MethodMetrics method, boolean empty) {
                super.updateItem(method, empty);
                if (empty || method == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label name = new Label(method.name());
                Label line = new Label(String.valueOf(method.startLine()));
                HBox row = new HBox(8);
                row.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(name, Priority.ALWAYS);
                row.getChildren().addAll(name, line);
                setGraphic(row);
                setText(null);
                setAccessibleText(method.name() + " line " + method.startLine());
            }
        });
        list.setOnMouseClicked(event -> {
            if (event.getClickCount() >= 1) {
                activateSelected();
            }
        });
        VBox.setVgrow(list, Priority.ALWAYS);
        root.getChildren().addAll(message, list);
        showNoDocument();
    }

    /** Runs the go-to-line action for the selected row (also used by tests). */
    public void activateSelected() {
        MethodMetrics selected = list.getSelectionModel().getSelectedItem();
        if (selected != null) {
            onGoToLine.accept(selected.startLine());
        }
    }

    public VBox root() {
        return root;
    }

    public Label message() {
        return message;
    }

    public ListView<MethodMetrics> list() {
        return list;
    }

    public void setOnGoToLine(IntConsumer onGoToLine) {
        this.onGoToLine = Objects.requireNonNull(onGoToLine);
    }

    public void showNoDocument() {
        showMessage(NO_DOCUMENT);
    }

    public void showNoOutline() {
        showMessage(NO_OUTLINE);
    }

    public void showAnalyzing() {
        showMessage(ANALYZING);
    }

    public void showMethods(List<MethodMetrics> methods) {
        List<MethodMetrics> rows = methods == null ? List.of() : List.copyOf(methods);
        if (rows.isEmpty()) {
            showAnalyzing();
            return;
        }
        message.setVisible(false);
        message.setManaged(false);
        list.setVisible(true);
        list.setManaged(true);
        list.getItems().setAll(rows);
    }

    private void showMessage(String text) {
        message.setText(text);
        message.setAccessibleText(text);
        message.setVisible(true);
        message.setManaged(true);
        list.getItems().clear();
        list.setVisible(false);
        list.setManaged(false);
    }
}
