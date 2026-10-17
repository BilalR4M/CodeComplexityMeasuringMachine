package dev.bilal.ccmm.app;

import dev.bilal.ccmm.app.document.Document;
import dev.bilal.ccmm.app.document.EditOrigin;
import dev.bilal.ccmm.app.session.Session;
import dev.bilal.ccmm.app.session.SessionStore;
import dev.bilal.ccmm.app.settings.ConfirmPrompt;
import dev.bilal.ccmm.app.settings.Settings;
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
import dev.bilal.ccmm.app.workbench.Theme;
import dev.bilal.ccmm.app.workbench.Workbench;
import dev.bilal.ccmm.collab.protocol.Message;
import dev.bilal.ccmm.core.AnalysisResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    private final SessionStore sessionStore;
    private final ExecutorService sessionIo = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "ccmm-session");
        thread.setDaemon(true);
        return thread;
    });
    private final Scene scene = new Scene(workbench.root(), 1280, 800);
    private final List<String> pinnedPaths = new ArrayList<>();
    private Runnable onListed = () -> {};
    private Runnable onOpened = () -> {};
    private Runnable onAnalyzed = () -> {};
    private Runnable onRestored = () -> {};
    private Path projectRoot;
    private Document document;
    private AnalysisResult latest;
    private AnalysisResult.Ok lastGood;
    private int openGeneration;
    private boolean remoteFlushScheduled;
    private boolean syncingEditor;
    private boolean welcome = true;

    public IdeWindow() {
        this(AnalysisScheduler.QUIET_MILLIS, DirectoryPrompt.chooser(), SettingsStore.defaultDirectory());
    }

    IdeWindow(long analysisDelayMs) {
        this(analysisDelayMs, DirectoryPrompt.chooser(), SettingsStore.defaultDirectory());
    }

    IdeWindow(long analysisDelayMs, DirectoryPrompt directoryPrompt) {
        this(analysisDelayMs, directoryPrompt, SettingsStore.defaultDirectory());
    }

    IdeWindow(long analysisDelayMs, DirectoryPrompt directoryPrompt, Path configDirectory) {
        this(
                analysisDelayMs,
                directoryPrompt,
                new SettingsStore(configDirectory),
                new SessionStore(configDirectory));
    }

    IdeWindow(long analysisDelayMs, DirectoryPrompt directoryPrompt, SettingsStore settingsStore) {
        this(
                analysisDelayMs,
                directoryPrompt,
                settingsStore,
                new SessionStore(settingsStore.file().getParent()));
    }

    IdeWindow(
            long analysisDelayMs,
            DirectoryPrompt directoryPrompt,
            SettingsStore settingsStore,
            SessionStore sessionStore) {
        this.settingsStore = Objects.requireNonNull(settingsStore);
        this.sessionStore = Objects.requireNonNull(sessionStore);
        this.settingsPanel = new SettingsPanel(settingsStore, ConfirmPrompt.dialog());
        this.settingsPanel.setOnThemeChanged(this::applySettingsTheme);
        analysis = new AnalysisScheduler(analysisDelayMs, Platform::runLater);
        applySettingsTheme(settingsStore.current().theme());
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
        editor.textProperty().addListener((obs, previous, text) -> {
            syncDocumentFromEditor(text);
            scheduleAnalysis(text);
        });
        editor.caretPositionProperty().addListener((obs, previous, current) -> {
            panel.caretMoved(caretLine());
            updateCaretStatus();
        });
        editor.setUndoManager(UndoUtils.richTextSuspendableUndoManager(editor, localUndo));
        updateCaretStatus();
    }

    Document document() {
        return document;
    }

    public SessionStore sessionStore() {
        return sessionStore;
    }

    public List<String> pinnedPaths() {
        return List.copyOf(pinnedPaths);
    }

    public boolean isWelcome() {
        return welcome;
    }

    public Path projectRoot() {
        return projectRoot;
    }

    public void setOnRestored(Runnable onRestored) {
        this.onRestored = Objects.requireNonNull(onRestored);
    }

    /**
     * Loads session.properties after the stage is shown. Existence checks run off
     * the FX thread; a missing folder leaves the welcome state.
     */
    public void restoreSession() {
        sessionIo.execute(() -> {
            Session existing = sessionStore.existing(sessionStore.load());
            Platform.runLater(() -> applyRestoredSession(existing));
        });
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
        welcome = false;
        projectRoot = directory;
        sideViews.setFolderName(directory.getFileName() == null ? directory.toString() : directory.getFileName().toString());
        files.open(directory, node -> Platform.runLater(() -> {
            tree.setRoot(item(node));
            tree.getRoot().setExpanded(true);
            workbench.setStatus("");
            if (activityBar.selected() == ActivityView.SEARCH) {
                sideViews.show(ActivityView.SEARCH);
            }
            persistSession();
            onListed.run();
        }));
    }

    public void close() {
        persistSession();
        sessionIo.shutdown();
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
        if (file == null || projectRoot == null) {
            return;
        }
        String next = relative(file);
        if (!Document.isJavaPath(next)) {
            workbench.setStatus(Document.ONLY_JAVA_MESSAGE);
            return;
        }
        int ticket = ++openGeneration;
        files.read(file, text -> Platform.runLater(() -> {
            if (ticket != openGeneration || text == null) {
                return;
            }
            if (document == null || !document.relativePath().equals(next)) {
                lastGood = null;
                panel.showUnparsed("");
            }
            document = new Document(next, text);
            syncingEditor = true;
            try {
                editor.replaceText(text);
            } finally {
                syncingEditor = false;
            }
            pin(next);
            workbench.setStatus("");
            updateCaretStatus();
            onOpened.run();
        }));
    }

    private void applyRestoredSession(Session session) {
        workbench.show(RegionId.SIDE, session.sideBarVisible());
        activityBar.setEnabled(session.sideBarVisible());
        workbench.show(RegionId.BOTTOM, session.bottomVisible());
        workbench.show(RegionId.INSPECTOR, session.inspectorVisible());
        if (session.lastFolder() == null) {
            welcome = true;
            projectRoot = null;
            pinnedPaths.clear();
            onRestored.run();
            return;
        }
        pinnedPaths.clear();
        pinnedPaths.addAll(session.pinned());
        List<String> toOpen = List.copyOf(session.pinned());
        Runnable previousListed = onListed;
        setOnListed(() -> {
            setOnListed(previousListed);
            openPinnedQueue(toOpen, 0, () -> {
                previousListed.run();
                onRestored.run();
            });
        });
        open(session.lastFolder());
    }

    private void openPinnedQueue(List<String> pins, int index, Runnable done) {
        if (index >= pins.size() || projectRoot == null) {
            done.run();
            return;
        }
        Path file = projectRoot.resolve(pins.get(index));
        Runnable previousOpened = onOpened;
        setOnOpened(() -> {
            setOnOpened(previousOpened);
            openPinnedQueue(pins, index + 1, done);
        });
        openFile(file);
    }

    private void pin(String relativePath) {
        pinnedPaths.remove(relativePath);
        pinnedPaths.add(relativePath);
        while (pinnedPaths.size() > SessionStore.MAX_PINNED) {
            pinnedPaths.remove(0);
        }
        persistSession();
    }

    private void persistSession() {
        Session session = new Session(
                projectRoot,
                pinnedPaths,
                workbench.isShowing(RegionId.SIDE),
                workbench.isShowing(RegionId.BOTTOM),
                workbench.isShowing(RegionId.INSPECTOR));
        try {
            sessionStore.save(session);
        } catch (RuntimeException ignored) {
            // session write must not break close or open
        }
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
        String openPath = document == null ? null : document.relativePath();
        List<dev.bilal.ccmm.collab.protocol.TextOp> ops = RemoteEdits.accepted(projectRoot, openPath, batch);
        syncingEditor = true;
        try {
            RemoteEdits.apply(document, editor, localUndo, ops);
        } catch (IllegalArgumentException failure) {
            workbench.setStatus(failure.getMessage());
        } finally {
            syncingEditor = false;
        }
    }

    private void syncDocumentFromEditor(String text) {
        if (syncingEditor || document == null || text == null) {
            return;
        }
        if (text.equals(document.text())) {
            return;
        }
        try {
            document.replace(0, document.text().length(), text, EditOrigin.LOCAL);
        } catch (IllegalArgumentException failure) {
            workbench.setStatus(failure.getMessage());
        }
    }

    private void scheduleAnalysis(String text) {
        if (document == null || text == null || !Document.isJavaPath(document.relativePath())) {
            return;
        }
        analysis.schedule(text, document.relativePath(), this::publish);
    }

    private void publish(AnalysisResult result) {
        latest = result;
        String path = switch (result) {
            case AnalysisResult.Ok ok -> ok.relativePath();
            case AnalysisResult.Failed failed -> failed.relativePath();
        };
        if (document != null && document.relativePath().equals(path)) {
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

    private void applySettingsTheme(Settings.Theme settingsTheme) {
        workbench.applyTheme(settingsTheme == Settings.Theme.LIGHT ? Theme.LIGHT : Theme.DARK);
    }

    private static TreeItem<FileNode> item(FileNode node) {
        TreeItem<FileNode> item = new TreeItem<>(node);
        for (FileNode child : node.children()) {
            item.getChildren().add(item(child));
        }
        return item;
    }
}
