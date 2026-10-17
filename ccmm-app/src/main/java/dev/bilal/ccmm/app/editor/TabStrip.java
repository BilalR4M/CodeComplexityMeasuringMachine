package dev.bilal.ccmm.app.editor;

import java.util.Objects;
import java.util.function.IntConsumer;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/** Tab buttons over the editor plus an {@code Open tabs} overflow menu. */
public final class TabStrip {
    public static final String OPEN_TABS = "Open tabs";

    private final EditorTabs model;
    private final HBox root = new HBox(0);
    private final HBox tabsBox = new HBox(2);
    private final ScrollPane scroll = new ScrollPane(tabsBox);
    private final MenuButton openTabs = new MenuButton(OPEN_TABS);
    private IntConsumer onSelect = index -> {};
    private IntConsumer onClose = index -> {};

    public TabStrip(EditorTabs model) {
        this.model = Objects.requireNonNull(model);
        root.setId("tab-strip");
        scroll.setId("tab-strip-scroll");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setFitToHeight(true);
        scroll.setPrefHeight(28);
        openTabs.setId("tab-strip-open-tabs");
        openTabs.setText(OPEN_TABS);
        openTabs.setAccessibleText(OPEN_TABS);
        openTabs.setTooltip(new Tooltip(OPEN_TABS));
        HBox.setHgrow(scroll, Priority.ALWAYS);
        root.getChildren().addAll(scroll, openTabs);
        refresh();
    }

    public HBox root() {
        return root;
    }

    public MenuButton openTabsButton() {
        return openTabs;
    }

    public void setOnSelect(IntConsumer onSelect) {
        this.onSelect = Objects.requireNonNull(onSelect);
    }

    public void setOnClose(IntConsumer onClose) {
        this.onClose = Objects.requireNonNull(onClose);
    }

    public void refresh() {
        tabsBox.getChildren().clear();
        openTabs.getItems().clear();
        var tabs = model.tabs();
        openTabs.setVisible(!tabs.isEmpty());
        openTabs.setManaged(!tabs.isEmpty());
        for (int i = 0; i < tabs.size(); i++) {
            OpenTab tab = tabs.get(i);
            int index = i;
            Button button = new Button(tab.label());
            button.setId("tab-" + index);
            button.setPrefHeight(28);
            button.setMinHeight(28);
            button.setFocusTraversable(true);
            button.setAccessibleText(tab.accessibleName());
            button.setTooltip(new Tooltip(tab.relativePath()));
            if (tab.preview()) {
                button.setStyle("-fx-font-style: italic;");
            }
            if (index == model.activeIndex()) {
                button.getStyleClass().add("tab-active");
            }
            button.setOnAction(event -> onSelect.accept(index));
            button.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.MIDDLE) {
                    onClose.accept(index);
                    event.consume();
                }
            });
            tabsBox.getChildren().add(button);

            MenuItem item = new MenuItem(tab.accessibleName());
            item.setOnAction(event -> onSelect.accept(index));
            openTabs.getItems().add(item);
        }
    }
}
