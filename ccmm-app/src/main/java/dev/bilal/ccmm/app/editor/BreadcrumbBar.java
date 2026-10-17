package dev.bilal.ccmm.app.editor;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/** Path + method crumbs above the editor. */
public final class BreadcrumbBar {
    private final HBox root = new HBox(0);
    private List<Breadcrumbs.Segment> shown = List.of();
    private Consumer<String> onPathSegment = path -> {};

    public BreadcrumbBar() {
        root.setId("breadcrumb-bar");
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(2, 8, 2, 8));
        root.setVisible(false);
        root.setManaged(false);
    }

    public HBox root() {
        return root;
    }

    public void setOnPathSegment(Consumer<String> onPathSegment) {
        this.onPathSegment = Objects.requireNonNull(onPathSegment);
    }

    public void clear() {
        shown = List.of();
        root.getChildren().clear();
        root.setVisible(false);
        root.setManaged(false);
    }

    public void show(List<Breadcrumbs.Segment> segments) {
        root.getChildren().clear();
        if (segments == null || segments.isEmpty()) {
            clear();
            return;
        }
        shown = List.copyOf(segments);
        for (int i = 0; i < shown.size(); i++) {
            if (i > 0) {
                Label sep = new Label(">");
                sep.setPadding(new Insets(0, 6, 0, 6));
                root.getChildren().add(sep);
            }
            Breadcrumbs.Segment segment = shown.get(i);
            if (segment.pathSegment()) {
                Hyperlink link = new Hyperlink(segment.label());
                link.setId("breadcrumb-" + i);
                link.setAccessibleText(segment.label());
                String path = segment.relativePath();
                link.setOnAction(event -> onPathSegment.accept(path));
                root.getChildren().add(link);
            } else {
                Label method = new Label(segment.label());
                method.setId("breadcrumb-method");
                method.setAccessibleText(segment.label());
                root.getChildren().add(method);
            }
        }
        root.setVisible(true);
        root.setManaged(true);
    }

    public String text() {
        return Breadcrumbs.join(shown);
    }
}
