package dev.bilal.ccmm.app;

import java.nio.file.Files;
import java.nio.file.Path;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;

public final class IdeWindow {
    private final FileTreeController files = new FileTreeController();
    private final TreeView<String> tree = new TreeView<>();
    private final BorderPane root = new BorderPane();
    private final Scene scene = new Scene(root, 1100, 700);
    private Runnable onListed = () -> {};

    public IdeWindow() {
        tree.setShowRoot(true);
        MenuItem open = new MenuItem("Open Directory...");
        open.setOnAction(event -> chooseDirectory());
        Menu file = new Menu("File", null, open);
        root.setTop(new MenuBar(file));
        root.setLeft(tree);
        root.setCenter(new Label("No file open"));
    }

    public Scene scene() {
        return scene;
    }

    TreeItem<String> listedRoot() {
        return tree.getRoot();
    }

    public void setOnListed(Runnable onListed) {
        this.onListed = onListed;
    }

    public void open(Path directory) {
        if (directory == null || !Files.isDirectory(directory)) {
            return;
        }
        files.open(directory, node -> Platform.runLater(() -> {
            tree.setRoot(item(node));
            tree.getRoot().setExpanded(true);
            onListed.run();
        }));
    }

    public void close() {
        files.close();
    }

    private void chooseDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Open project");
        var selected = chooser.showDialog(tree.getScene() == null ? null : tree.getScene().getWindow());
        if (selected != null) {
            open(selected.toPath());
        }
    }

    private static TreeItem<String> item(FileNode node) {
        TreeItem<String> item = new TreeItem<>(node.name());
        for (FileNode child : node.children()) {
            item.getChildren().add(item(child));
        }
        return item;
    }
}
