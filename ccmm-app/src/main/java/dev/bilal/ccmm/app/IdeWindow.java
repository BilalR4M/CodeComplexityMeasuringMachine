package dev.bilal.ccmm.app;

import dev.bilal.ccmm.core.AnalysisResult;
import java.nio.file.Files;
import java.nio.file.Path;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import org.fxmisc.richtext.CodeArea;

public final class IdeWindow {
    private final FileTreeController files = new FileTreeController();
    private final AnalysisScheduler analysis;
    private final TreeView<FileNode> tree = new TreeView<>();
    private final CodeArea editor = new CodeArea();
    private final BorderPane root = new BorderPane();
    private final Scene scene = new Scene(root, 1100, 700);
    private Runnable onListed = () -> {};
    private Runnable onOpened = () -> {};
    private Runnable onAnalyzed = () -> {};
    private Path projectRoot;
    private String currentRelative;
    private AnalysisResult latest;
    private int openGeneration;

    public IdeWindow() {
        this(AnalysisScheduler.QUIET_MILLIS);
    }

    IdeWindow(long analysisDelayMs) {
        analysis = new AnalysisScheduler(analysisDelayMs, Platform::runLater);
        tree.setShowRoot(true);
        tree.setCellFactory(view -> new TreeCell<>() {
            @Override
            protected void updateItem(FileNode item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name());
            }
        });
        tree.getSelectionModel().selectedItemProperty().addListener((obs, previous, item) -> {
            if (item != null && item.getValue() != null && !item.getValue().directory()) {
                openFile(item.getValue().path());
            }
        });
        MenuItem open = new MenuItem("Open Directory...");
        open.setOnAction(event -> chooseDirectory());
        Menu file = new Menu("File", null, open);
        root.setTop(new MenuBar(file));
        root.setLeft(tree);
        root.setCenter(editor);
        editor.textProperty().addListener((obs, previous, text) -> scheduleAnalysis(text));
    }

    public Scene scene() {
        return scene;
    }

    public TreeItem<FileNode> listedRoot() {
        return tree.getRoot();
    }

    public void select(TreeItem<FileNode> item) {
        tree.getSelectionModel().select(item);
    }

    public CodeArea editor() {
        return editor;
    }

    public void setOnListed(Runnable onListed) {
        this.onListed = onListed;
    }

    public void setOnOpened(Runnable onOpened) {
        this.onOpened = onOpened;
    }

    public void setOnAnalyzed(Runnable onAnalyzed) {
        this.onAnalyzed = onAnalyzed;
    }

    AnalysisResult latestAnalysis() {
        return latest;
    }

    public void open(Path directory) {
        if (directory == null || !Files.isDirectory(directory)) {
            return;
        }
        projectRoot = directory;
        files.open(directory, node -> Platform.runLater(() -> {
            tree.setRoot(item(node));
            tree.getRoot().setExpanded(true);
            onListed.run();
        }));
    }

    public void close() {
        files.close();
        analysis.close();
    }

    private void openFile(Path file) {
        if (file == null || !file.getFileName().toString().endsWith(".java")) {
            return;
        }
        int ticket = ++openGeneration;
        files.read(file, text -> Platform.runLater(() -> {
            if (ticket != openGeneration || text == null) {
                return;
            }
            currentRelative = relative(file);
            editor.replaceText(text);
            onOpened.run();
        }));
    }

    private void scheduleAnalysis(String text) {
        if (currentRelative == null || text == null) {
            return;
        }
        analysis.schedule(text, currentRelative, this::publish);
    }

    private void publish(AnalysisResult result) {
        latest = result;
        onAnalyzed.run();
    }

    private String relative(Path file) {
        Path root = projectRoot.toAbsolutePath().normalize();
        Path target = file.toAbsolutePath().normalize();
        return root.relativize(target).toString().replace('\\', '/');
    }

    private void chooseDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Open project");
        var selected = chooser.showDialog(tree.getScene() == null ? null : tree.getScene().getWindow());
        if (selected != null) {
            open(selected.toPath());
        }
    }

    private static TreeItem<FileNode> item(FileNode node) {
        TreeItem<FileNode> item = new TreeItem<>(node);
        for (FileNode child : node.children()) {
            item.getChildren().add(item(child));
        }
        return item;
    }
}
