package dev.bilal.ccmm.app.workbench;

import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.StackPane;

/**
 * Collapsible bottom panel with Problems, Metrics, and Chat tabs.
 * Terminal is E6-T03. Empty copy only in this issue.
 */
public final class BottomPanel extends StackPane {
    public static final String PROBLEMS_EMPTY = "No problems";
    public static final String METRICS_EMPTY = "Open a folder to see project metrics";
    public static final String CHAT_EMPTY = "Not sharing";

    private final TabPane tabs = new TabPane();
    private final Label problems = empty(PROBLEMS_EMPTY, "problems-empty");
    private final Label metrics = empty(METRICS_EMPTY, "metrics-empty");
    private final Label chat = empty(CHAT_EMPTY, "chat-empty");

    public BottomPanel() {
        setId("bottom-panel");
        setMinHeight(Workbench.BOTTOM_MIN);
        setPrefHeight(Workbench.BOTTOM_DEFAULT);
        tabs.setId("bottom-tabs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(
                tab("Problems", problems),
                tab("Metrics", metrics),
                tab("Chat", chat));
        getChildren().add(tabs);
    }

    public TabPane tabs() {
        return tabs;
    }

    public String problemsText() {
        return problems.getText();
    }

    public void selectProblems() {
        tabs.getSelectionModel().select(0);
    }

    private static Tab tab(String title, Label content) {
        Tab tab = new Tab(title, content);
        tab.setClosable(false);
        return tab;
    }

    private static Label empty(String text, String id) {
        Label label = new Label(text);
        label.setId(id);
        label.setWrapText(true);
        label.setAccessibleText(text);
        return label;
    }
}
