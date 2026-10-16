package dev.bilal.ccmm.app.workbench;

import java.util.Objects;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Side bar chrome: title for the active view and a collapse control.
 * Content is the active SideViews host.
 */
public final class SideBar extends VBox {
    public static final String HIDE_SIDE_BAR = "Hide side bar";

    private final Label title = new Label(ActivityView.EXPLORER.title());
    private final Button collapse = new Button("«");
    private final StackPane content = new StackPane();
    private Consumer<Void> onCollapse = ignored -> {};

    public SideBar() {
        setId("side-bar");
        title.setId("side-bar-title");
        title.setStyle("-fx-font-weight: bold;");
        collapse.setId("side-bar-collapse");
        collapse.setPrefSize(24, 24);
        collapse.setMinSize(24, 24);
        collapse.setTooltip(new Tooltip(HIDE_SIDE_BAR));
        collapse.setAccessibleText(HIDE_SIDE_BAR);
        collapse.setOnAction(event -> onCollapse.accept(null));

        BorderPane header = new BorderPane();
        header.setId("side-bar-header");
        header.setLeft(title);
        header.setRight(collapse);
        BorderPane.setAlignment(title, Pos.CENTER_LEFT);
        BorderPane.setAlignment(collapse, Pos.CENTER_RIGHT);
        header.setPadding(new Insets(4, 4, 4, 8));
        HBox.setHgrow(title, Priority.ALWAYS);

        VBox.setVgrow(content, Priority.ALWAYS);
        getChildren().addAll(header, content);
    }

    public void setContent(Node node) {
        content.getChildren().setAll(node);
    }

    public void setActiveView(ActivityView view) {
        title.setText(Objects.requireNonNull(view).title());
    }

    public String titleText() {
        return title.getText();
    }

    public Button collapseButton() {
        return collapse;
    }

    public void setOnCollapse(Runnable onCollapse) {
        this.onCollapse = ignored -> Objects.requireNonNull(onCollapse).run();
    }
}
