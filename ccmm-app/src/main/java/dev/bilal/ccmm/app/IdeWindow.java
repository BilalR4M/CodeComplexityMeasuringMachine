package dev.bilal.ccmm.app;

import dev.bilal.ccmm.app.workbench.ActivityBar;
import dev.bilal.ccmm.app.workbench.ActivityView;
import dev.bilal.ccmm.app.workbench.RegionId;
import dev.bilal.ccmm.app.workbench.SideViews;
import dev.bilal.ccmm.app.workbench.Workbench;
import dev.bilal.ccmm.collab.protocol.Message;
import dev.bilal.ccmm.core.AnalysisResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.util.UndoUtils;
import org.reactfx.SuspendableYes;

public final class IdeWindow {
    private final FileTreeController files = new FileTreeController();
    private final AnalysisScheduler analysis;
    private final TreeView<FileNode> tree = new TreeView<>();
    private final CodeArea editor = new CodeArea();
    private final SuspendableYes localUndo = new SuspendableYes();
    private final List<Message> remotePending = new ArrayList<>();
    private final MetricsPanel panel = new MetricsPanel();
    private final ProjectRoomPanel room = new ProjectRoomPanel();
    private final Workbench workbench = new Workbench();
    private final ActivityBar activityBar = new ActivityBar();
    private final SideViews sideViews = new SideViews();
    private final Scene scene = new Scene(workbench.root(), 1280, 800);
    private Runnable onListed = () -> {};
    private Runnable onOpened = () -> {};
    private Runnable onAnalyzed = () -> {};
    private Path projectRoot;
    private String currentRelative;
    private AnalysisResult latest;
    private AnalysisResult.Ok lastGood;
    private int openGeneration;
    private boolean remoteFlushScheduled;

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
        workbench.setMenuBar(new MenuBar(file));
        sideViews.setExplorer(tree);
        sideViews.show(ActivityView.EXPLORER);
        activityBar.setOnSelect(view -> {
            sideViews.show(view);
            workbench.setContent(RegionId.SIDE, sideViews.host());
            if (!workbench.isShowing(RegionId.SIDE)) {
                workbench.show(RegionId.SIDE, true);
                activityBar.setEnabled(true);
            }
        });
        workbench.setContent(RegionId.ACTIVITY, activityBar);
        workbench.setContent(RegionId.SIDE, sideViews.host());
        workbench.setContent(RegionId.EDITOR, editor);
        workbench.setContent(RegionId.INSPECTOR, new VBox(panel, room));
        workbench.setContent(RegionId.BOTTOM, new Label("Problems"));
        wireActivityShortcuts();
        editor.textProperty().addListener((obs, previous, text) -> scheduleAnalysis(text));
        editor.caretPositionProperty().addListener((obs, previous, current) -> panel.caretMoved(caretLine()));
        editor.setUndoManager(UndoUtils.richTextSuspendableUndoManager(editor, localUndo));
    }

    public ActivityBar activityBar() {
        return activityBar;
    }

    public SideViews sideViews() {
        return sideViews;
    }

    public Scene scene() {
        return scene;
    }

    public Workbench workbench() {
        return workbench;
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

    String panelText() {
        return panel.text();
    }

    String roomText() {
        return room.text();
    }

    public void open(Path directory) {
        if (directory == null || !Files.isDirectory(directory)) {
            workbench.setStatus("Could not open folder");
            return;
        }
        workbench.setStatus("Opening…");
        projectRoot = directory;
        sideViews.setFolderName(directory.getFileName() == null ? directory.toString() : directory.getFileName().toString());
        files.open(directory, node -> Platform.runLater(() -> {
            tree.setRoot(item(node));
            tree.getRoot().setExpanded(true);
            workbench.setStatus("");
            if (activityBar.selected() == ActivityView.SEARCH) {
                sideViews.show(ActivityView.SEARCH);
            }
            onListed.run();
        }));
    }

    public void close() {
        files.close();
        analysis.close();
    }

    void applyRemote(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        if (Platform.isFxApplicationThread()) {
            enqueueRemote(messages);
        } else {
            Platform.runLater(() -> enqueueRemote(messages));
        }
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
            String next = relative(file);
            if (currentRelative == null || !currentRelative.equals(next)) {
                lastGood = null;
                panel.showUnparsed("");
            }
            currentRelative = next;
            editor.replaceText(text);
            onOpened.run();
        }));
    }

    private void enqueueRemote(List<Message> messages) {
        remotePending.addAll(messages);
        if (!remoteFlushScheduled) {
            remoteFlushScheduled = true;
            Platform.runLater(this::flushRemote);
        }
    }

    private void flushRemote() {
        List<Message> batch = List.copyOf(remotePending);
        remotePending.clear();
        remoteFlushScheduled = false;
        room.acceptAll(batch);
        RemoteEdits.apply(editor, localUndo, RemoteEdits.accepted(projectRoot, currentRelative, batch));
    }

    private void scheduleAnalysis(String text) {
        if (currentRelative == null || text == null) {
            return;
        }
        analysis.schedule(text, currentRelative, this::publish);
    }

    private void publish(AnalysisResult result) {
        latest = result;
        String path = switch (result) {
            case AnalysisResult.Ok ok -> ok.relativePath();
            case AnalysisResult.Failed failed -> failed.relativePath();
        };
        if (currentRelative != null && currentRelative.equals(path)) {
            switch (result) {
                case AnalysisResult.Ok ok -> {
                    lastGood = ok;
                    panel.showFresh(ok, caretLine());
                }
                case AnalysisResult.Failed failed -> {
                    if (lastGood != null && path.equals(lastGood.relativePath())) {
                        panel.showStale(lastGood, caretLine());
                    } else {
                        lastGood = null;
                        panel.showUnparsed(failed.message());
                    }
                }
            }
        }
        onAnalyzed.run();
    }

    private int caretLine() {
        return editor.getCurrentParagraph() + 1;
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

    /** Temporary accelerators. E1-T07 moves them into the keymap and deletes these. */
    private void wireActivityShortcuts() {
        scene.getAccelerators()
                .put(new KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN),
                        () -> activityBar.select(ActivityView.EXPLORER));
        scene.getAccelerators()
                .put(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN),
                        () -> activityBar.select(ActivityView.SEARCH));
        scene.getAccelerators()
                .put(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN),
                        () -> activityBar.select(ActivityView.OUTLINE));
        scene.getAccelerators()
                .put(new KeyCodeCombination(KeyCode.COMMA, KeyCombination.CONTROL_DOWN),
                        () -> activityBar.select(ActivityView.SETTINGS));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.B, KeyCombination.CONTROL_DOWN), () -> {
            boolean show = !workbench.isShowing(RegionId.SIDE);
            workbench.show(RegionId.SIDE, show);
            activityBar.setEnabled(show);
        });
    }

    private static TreeItem<FileNode> item(FileNode node) {
        TreeItem<FileNode> item = new TreeItem<>(node);
        for (FileNode child : node.children()) {
            item.getChildren().add(item(child));
        }
        return item;
    }
}
