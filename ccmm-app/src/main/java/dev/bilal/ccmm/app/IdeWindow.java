package dev.bilal.ccmm.app;

import dev.bilal.ccmm.app.settings.ConfirmPrompt;
import dev.bilal.ccmm.app.settings.SettingsPanel;
import dev.bilal.ccmm.app.settings.SettingsStore;
import dev.bilal.ccmm.app.workbench.ActivityBar;
import dev.bilal.ccmm.app.workbench.ActivityView;
import dev.bilal.ccmm.app.workbench.BottomPanel;
import dev.bilal.ccmm.app.workbench.CommandId;
import dev.bilal.ccmm.app.workbench.DirectoryPrompt;
import dev.bilal.ccmm.app.workbench.Keymap;
import dev.bilal.ccmm.app.workbench.OpenFolderCommand;
import dev.bilal.ccmm.app.workbench.RegionId;
import dev.bilal.ccmm.app.workbench.SideBar;
import dev.bilal.ccmm.app.workbench.SideViews;
import dev.bilal.ccmm.app.workbench.SimpleCommand;
import dev.bilal.ccmm.app.workbench.Workbench;
import dev.bilal.ccmm.collab.protocol.Message;
import dev.bilal.ccmm.core.AnalysisResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.VBox;
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
    private final SideBar sideBar = new SideBar();
    private final BottomPanel bottomPanel = new BottomPanel();
    private final Keymap keymap = Keymap.seeded(Keymap.detectPlatform());
    private final SettingsStore settingsStore;
    private final SettingsPanel settingsPanel;
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
        this(AnalysisScheduler.QUIET_MILLIS, DirectoryPrompt.chooser(), new SettingsStore(SettingsStore.defaultDirectory()));
    }

    IdeWindow(long analysisDelayMs) {
        this(analysisDelayMs, DirectoryPrompt.chooser(), new SettingsStore(SettingsStore.defaultDirectory()));
    }

    IdeWindow(long analysisDelayMs, DirectoryPrompt directoryPrompt) {
        this(analysisDelayMs, directoryPrompt, new SettingsStore(SettingsStore.defaultDirectory()));
    }

    IdeWindow(long analysisDelayMs, DirectoryPrompt directoryPrompt, SettingsStore settingsStore) {
        this.settingsStore = Objects.requireNonNull(settingsStore);
        this.settingsPanel = new SettingsPanel(settingsStore, ConfirmPrompt.dialog());
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
        workbench.register(new OpenFolderCommand(directoryPrompt, this::open, () -> scene.getWindow()));
        registerViewCommands();
        MenuItem open = new MenuItem("Open Folder…");
        open.setOnAction(event -> workbench.execute(CommandId.FILE_OPEN.value()));
        Menu file = new Menu("File", null, open);
        workbench.setMenuBar(new MenuBar(file));
        sideViews.setExplorer(tree);
        sideViews.setSettings(settingsPanel.root());
        sideViews.show(ActivityView.EXPLORER);
        sideBar.setContent(sideViews.host());
        sideBar.setActiveView(ActivityView.EXPLORER);
        sideBar.setOnCollapse(this::toggleSideBar);
        activityBar.setOnSelect(view -> {
            sideViews.show(view);
            sideBar.setContent(sideViews.host());
            sideBar.setActiveView(view);
            if (!workbench.isShowing(RegionId.SIDE)) {
                workbench.show(RegionId.SIDE, true);
                activityBar.setEnabled(true);
            }
        });
        workbench.setContent(RegionId.ACTIVITY, activityBar);
        workbench.setContent(RegionId.SIDE, sideBar);
        workbench.setContent(RegionId.EDITOR, editor);
        workbench.setContent(RegionId.INSPECTOR, new VBox(panel, room));
        workbench.setContent(RegionId.BOTTOM, bottomPanel);
        keymap.install(scene, workbench);
        editor.textProperty().addListener((obs, previous, text) -> scheduleAnalysis(text));
        editor.caretPositionProperty().addListener((obs, previous, current) -> {
            panel.caretMoved(caretLine());
            updateCaretStatus();
        });
        editor.setUndoManager(UndoUtils.richTextSuspendableUndoManager(editor, localUndo));
        updateCaretStatus();
    }

    public ActivityBar activityBar() {
        return activityBar;
    }

    public SideViews sideViews() {
        return sideViews;
    }

    public SideBar sideBar() {
        return sideBar;
    }

    public BottomPanel bottomPanel() {
        return bottomPanel;
    }

    public Keymap keymap() {
        return keymap;
    }

    public SettingsStore settingsStore() {
        return settingsStore;
    }

    public SettingsPanel settingsPanel() {
        return settingsPanel;
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
            updateCaretStatus();
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

    private void updateCaretStatus() {
        workbench.statusBar().setCaret(caretLine(), editor.getCaretColumn() + 1);
    }

    private String relative(Path file) {
        Path root = projectRoot.toAbsolutePath().normalize();
        Path target = file.toAbsolutePath().normalize();
        return root.relativize(target).toString().replace('\\', '/');
    }

    private void registerViewCommands() {
        workbench.register(new SimpleCommand(CommandId.VIEW_TOGGLE_SIDE, "Toggle Side Bar", wb -> toggleSideBar()));
        workbench.register(
                new SimpleCommand(CommandId.VIEW_TOGGLE_BOTTOM, "Toggle Bottom Panel", wb -> toggleBottomPanel()));
        workbench.register(new SimpleCommand(
                CommandId.VIEW_EXPLORER, "Explorer", wb -> activityBar.select(ActivityView.EXPLORER)));
        workbench.register(
                new SimpleCommand(CommandId.VIEW_SEARCH, "Search", wb -> activityBar.select(ActivityView.SEARCH)));
        workbench.register(
                new SimpleCommand(CommandId.VIEW_OUTLINE, "Outline", wb -> activityBar.select(ActivityView.OUTLINE)));
        workbench.register(new SimpleCommand(
                CommandId.VIEW_SETTINGS, "Settings", wb -> activityBar.select(ActivityView.SETTINGS)));
    }

    private void toggleSideBar() {
        boolean show = !workbench.isShowing(RegionId.SIDE);
        workbench.show(RegionId.SIDE, show);
        activityBar.setEnabled(show);
    }

    private void toggleBottomPanel() {
        workbench.show(RegionId.BOTTOM, !workbench.isShowing(RegionId.BOTTOM));
    }

    private static TreeItem<FileNode> item(FileNode node) {
        TreeItem<FileNode> item = new TreeItem<>(node);
        for (FileNode child : node.children()) {
            item.getChildren().add(item(child));
        }
        return item;
    }
}
