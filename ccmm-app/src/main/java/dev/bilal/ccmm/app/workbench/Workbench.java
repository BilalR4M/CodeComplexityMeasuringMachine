package dev.bilal.ccmm.app.workbench;

import java.util.EnumMap;
import java.util.Map;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Shell with activity bar, side bar, editor, inspector, bottom panel, and status bar.
 * FX thread only. Reparents content nodes; does not own analysis or file IO.
 */
public final class Workbench {
    static final double ACTIVITY_WIDTH = 48;
    static final double SIDE_DEFAULT = 260;
    static final double INSPECTOR_DEFAULT = 280;
    static final double BOTTOM_DEFAULT = 180;
    static final double SIDE_MIN = 160;
    static final double INSPECTOR_MIN = 200;
    static final double BOTTOM_MIN = 80;
    static final double EDITOR_SHARE = 0.65;
    static final double INSPECTOR_COLLAPSE_WIDTH = 1100;

    private final BorderPane root = new BorderPane();
    private final VBox activity = regionBox("activity");
    private final StackPane side = regionStack("side");
    private final StackPane editor = regionStack("editor");
    private final StackPane inspector = regionStack("inspector");
    private final StackPane bottom = regionStack("bottom");
    private final Label status = new Label();
    private final SplitPane vertical = new SplitPane();
    private final SplitPane horizontal = new SplitPane();
    private final BorderPane center = new BorderPane();
    private final Map<RegionId, Boolean> visible = new EnumMap<>(RegionId.class);
    private double sideWidth = SIDE_DEFAULT;
    private double inspectorWidth = INSPECTOR_DEFAULT;
    private double bottomHeight = BOTTOM_DEFAULT;

    public Workbench() {
        for (RegionId id : RegionId.values()) {
            visible.put(id, true);
        }
        activity.setPrefWidth(ACTIVITY_WIDTH);
        activity.setMinWidth(ACTIVITY_WIDTH);
        activity.setMaxWidth(ACTIVITY_WIDTH);
        side.setMinWidth(SIDE_MIN);
        inspector.setMinWidth(INSPECTOR_MIN);
        bottom.setMinHeight(BOTTOM_MIN);
        status.setId("status-bar");
        status.setAccessibleText("Status");
        status.setMinHeight(24);
        status.setMaxHeight(24);
        status.setPrefHeight(24);

        vertical.getItems().addAll(editor, bottom);
        vertical.setOrientation(Orientation.VERTICAL);
        horizontal.getItems().addAll(side, vertical, inspector);
        horizontal.setOrientation(Orientation.HORIZONTAL);

        center.setLeft(activity);
        center.setCenter(horizontal);
        root.setCenter(center);
        root.setBottom(status);

        bindDividerCollapse(horizontal, 0, RegionId.SIDE);
        bindDividerCollapse(horizontal, 1, RegionId.INSPECTOR);
        bindDividerCollapse(vertical, 0, RegionId.BOTTOM);

        root.widthProperty().addListener((obs, previous, width) -> applyWidth(width.doubleValue()));
        root.sceneProperty().addListener((obs, previous, scene) -> {
            if (scene != null) {
                applyWidth(scene.getWidth());
            }
        });
    }

    public BorderPane root() {
        return root;
    }

    public Label statusBar() {
        return status;
    }

    public void setStatus(String text) {
        status.setText(text == null ? "" : text);
    }

    public void setMenuBar(Node menuBar) {
        root.setTop(menuBar);
    }

    public void setContent(RegionId region, Node content) {
        switch (region) {
            case ACTIVITY -> {
                activity.getChildren().setAll(content);
                VBox.setVgrow(content, Priority.ALWAYS);
            }
            case SIDE -> side.getChildren().setAll(content);
            case EDITOR -> editor.getChildren().setAll(content);
            case INSPECTOR -> inspector.getChildren().setAll(content);
            case BOTTOM -> bottom.getChildren().setAll(content);
            case STATUS -> status.setText(content instanceof Label label ? label.getText() : "");
        }
    }

    public void show(RegionId region, boolean show) {
        if (region == RegionId.EDITOR || region == RegionId.STATUS || region == RegionId.ACTIVITY) {
            visible.put(region, true);
            return;
        }
        visible.put(region, show);
        rebuildHorizontal();
        rebuildVertical();
        applyWidth(root.getWidth() > 0 ? root.getWidth() : sceneWidth());
    }

    public boolean isShowing(RegionId region) {
        return Boolean.TRUE.equals(visible.get(region));
    }

    /** Width of the editor column (editor + bottom stack), for layout tests. */
    public double editorColumnWidth() {
        return vertical.getWidth();
    }

    private void applyWidth(double width) {
        if (width <= 0) {
            return;
        }
        if (width < INSPECTOR_COLLAPSE_WIDTH && isShowing(RegionId.INSPECTOR)) {
            visible.put(RegionId.INSPECTOR, false);
            rebuildHorizontal();
        }
        // Editor column (editor + bottom) must keep EDITOR_SHARE of the window width.
        // Reserve a few pixels for SplitPane dividers so the measured editor share stays ≥ 65%.
        double available = width - ACTIVITY_WIDTH;
        double maxSideInspector = Math.max(0, width * (1.0 - EDITOR_SHARE) - ACTIVITY_WIDTH - 12);
        double side = isShowing(RegionId.SIDE) ? Math.min(sideWidth, available) : 0;
        double insp = isShowing(RegionId.INSPECTOR) ? Math.min(inspectorWidth, available) : 0;
        double chrome = side + insp;
        if (chrome > maxSideInspector && chrome > 0) {
            double scale = maxSideInspector / chrome;
            side *= scale;
            insp *= scale;
        }
        if (isShowing(RegionId.SIDE)) {
            sideWidth = Math.max(1, side);
            this.side.setPrefWidth(sideWidth);
        }
        if (isShowing(RegionId.INSPECTOR)) {
            inspectorWidth = Math.max(1, insp);
            inspector.setPrefWidth(inspectorWidth);
        }
        setHorizontalDividers(sideWidth, inspectorWidth, available);
        setVerticalDivider(bottomHeight);
    }

    private void setHorizontalDividers(double sidePx, double inspectorPx, double available) {
        if (horizontal.getItems().isEmpty() || available <= 0) {
            return;
        }
        boolean hasSide = horizontal.getItems().contains(this.side);
        boolean hasInsp = horizontal.getItems().contains(inspector);
        if (hasSide && hasInsp) {
            horizontal.setDividerPositions(sidePx / available, (available - inspectorPx) / available);
        } else if (hasSide) {
            horizontal.setDividerPositions(sidePx / available);
        } else if (hasInsp) {
            horizontal.setDividerPositions((available - inspectorPx) / available);
        }
    }

    private void setVerticalDivider(double bottomPx) {
        if (vertical.getItems().size() < 2) {
            return;
        }
        double height = vertical.getHeight();
        if (height <= 0) {
            height = root.getHeight() > 0 ? root.getHeight() - 48 : 700;
        }
        double pos = Math.max(0.2, 1.0 - bottomPx / height);
        vertical.setDividerPositions(pos);
    }

    private void rebuildHorizontal() {
        horizontal.getItems().clear();
        if (isShowing(RegionId.SIDE)) {
            horizontal.getItems().add(side);
        }
        horizontal.getItems().add(vertical);
        if (isShowing(RegionId.INSPECTOR)) {
            horizontal.getItems().add(inspector);
        }
    }

    private void rebuildVertical() {
        vertical.getItems().clear();
        vertical.getItems().add(editor);
        if (isShowing(RegionId.BOTTOM)) {
            vertical.getItems().add(bottom);
        }
    }

    private void bindDividerCollapse(SplitPane pane, int dividerIndex, RegionId region) {
        pane.sceneProperty().addListener((obs, previous, scene) -> {
            if (scene == null) {
                return;
            }
            scene.getWindow().showingProperty().addListener((o, was, showing) -> {
                if (showing) {
                    attachCollapseHandler(pane, dividerIndex, region);
                }
            });
        });
    }

    private void attachCollapseHandler(SplitPane pane, int dividerIndex, RegionId region) {
        pane.lookupAll(".split-pane-divider").stream()
                .skip(dividerIndex)
                .findFirst()
                .ifPresent(divider -> divider.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2) {
                        show(region, false);
                        event.consume();
                    }
                }));
    }

    private double sceneWidth() {
        return root.getScene() == null ? 0 : root.getScene().getWidth();
    }

    private static StackPane regionStack(String id) {
        StackPane pane = new StackPane();
        pane.setId(id);
        return pane;
    }

    private static VBox regionBox(String id) {
        VBox box = new VBox();
        box.setId(id);
        box.setMinWidth(ACTIVITY_WIDTH);
        return box;
    }

}
