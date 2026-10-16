package dev.bilal.ccmm.app.workbench;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

/** Hosts the five side-bar views. Explorer content is supplied by IdeWindow. */
public final class SideViews {
    private final StackPane host = new StackPane();
    private Node explorer;
    private Node settings;
    private String folderName;

    public SideViews() {
        host.setId("side-views");
    }

    public Node host() {
        return host;
    }

    public void setExplorer(Node explorer) {
        this.explorer = explorer;
    }

    public void setSettings(Node settings) {
        this.settings = settings;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public void show(ActivityView view) {
        host.getChildren().setAll(content(view));
    }

    public String emptyCopy(ActivityView view) {
        return SideViewCopy.empty(view, folderName);
    }

    private Node content(ActivityView view) {
        if (view == ActivityView.EXPLORER) {
            return explorer == null ? label(SideViewCopy.empty(ActivityView.EXPLORER, null), "explorer") : explorer;
        }
        if (view == ActivityView.SETTINGS) {
            return settings == null ? label(emptyCopy(ActivityView.SETTINGS), "settings") : settings;
        }
        return label(emptyCopy(view), view.name().toLowerCase());
    }

    private static Label label(String text, String idSuffix) {
        Label label = new Label(text);
        label.setId("side-empty-" + idSuffix);
        label.setWrapText(true);
        label.setAccessibleText(text);
        return label;
    }
}
