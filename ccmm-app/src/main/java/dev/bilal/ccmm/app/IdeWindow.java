package dev.bilal.ccmm.app;

import dev.bilal.ccmm.app.document.Document;
import dev.bilal.ccmm.app.document.EditOrigin;
import dev.bilal.ccmm.app.editor.AutoIndent;
import dev.bilal.ccmm.app.editor.BracketMatcher;
import dev.bilal.ccmm.app.editor.BufferFinder;
import dev.bilal.ccmm.app.editor.EditorTabs;
import dev.bilal.ccmm.app.editor.FindReplaceBar;
import dev.bilal.ccmm.app.editor.GoToLine;
import dev.bilal.ccmm.app.editor.GoToLineBar;
import dev.bilal.ccmm.app.editor.OpenTab;
import dev.bilal.ccmm.app.editor.SaveClosePrompt;
import dev.bilal.ccmm.app.editor.TabStrip;
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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
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
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Priority;
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
    private final EditorTabs editorTabs = new EditorTabs();
    private final TabStrip tabStrip = new TabStrip(editorTabs);
    private final FindReplaceBar findReplaceBar = new FindReplaceBar();
    private final GoToLineBar goToLineBar = new GoToLineBar();
    private final SettingsStore settingsStore;
    private final SettingsPanel settingsPanel;
    private final SessionStore sessionStore;
    private final SaveClosePrompt saveClosePrompt;
    private final ExecutorService backgroundIo = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "ccmm-background-io");
        thread.setDaemon(true);
        return thread;
    });
    private final Scene scene = new Scene(workbench.root(), 1280, 800);
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
    private List<BufferFinder.Match> findMatches = List.of();
    private int findIndex = -1;
    private int findGeneration;
    private int bracketOpen = -1;
    private int bracketClose = -1;
    private int bracketGeneration;

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
                new SessionStore(configDirectory),
                SaveClosePrompt.dialog());
    }

    IdeWindow(long analysisDelayMs, DirectoryPrompt directoryPrompt, SettingsStore settingsStore) {
        this(
                analysisDelayMs,
                directoryPrompt,
                settingsStore,
                new SessionStore(settingsStore.file().getParent()),
                SaveClosePrompt.dialog());
    }

    IdeWindow(
            long analysisDelayMs,
            DirectoryPrompt directoryPrompt,
            SettingsStore settingsStore,
            SessionStore sessionStore) {
        this(analysisDelayMs, directoryPrompt, settingsStore, sessionStore, SaveClosePrompt.dialog());
    }

    IdeWindow(
            long analysisDelayMs,
            DirectoryPrompt directoryPrompt,
            SettingsStore settingsStore,
            SaveClosePrompt saveClosePrompt) {
        this(
                analysisDelayMs,
                directoryPrompt,
                settingsStore,
                new SessionStore(settingsStore.file().getParent()),
                saveClosePrompt);
    }

    IdeWindow(
            long analysisDelayMs,
            DirectoryPrompt directoryPrompt,
            SettingsStore settingsStore,
            SessionStore sessionStore,
            SaveClosePrompt saveClosePrompt) {
        this.settingsStore = Objects.requireNonNull(settingsStore);
        this.sessionStore = Objects.requireNonNull(sessionStore);
        this.saveClosePrompt = Objects.requireNonNull(saveClosePrompt);
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
        tree.setOnMouseClicked(event -> {
            TreeItem<FileNode> item = tree.getSelectionModel().getSelectedItem();
            if (item == null || item.getValue() == null || item.getValue().directory()) {
                return;
            }
            boolean pin = event.getButton() == MouseButton.PRIMARY && event.getClickCount() >= 2;
            openFile(item.getValue().path(), pin);
        });
        tree.getSelectionModel().selectedItemProperty().addListener((obs, previous, item) -> {
            if (item != null && item.getValue() != null && !item.getValue().directory()) {
                openFile(item.getValue().path(), false);
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
        tabStrip.setOnSelect(this::activateTab);
        tabStrip.setOnClose(this::requestCloseTab);
        findReplaceBar.setOnNext(this::findNext);
        findReplaceBar.setOnPrevious(this::findPrevious);
        findReplaceBar.setOnReplace(this::replaceCurrent);
        findReplaceBar.setOnReplaceAll(this::replaceAllMatches);
        findReplaceBar.setOnClose(findReplaceBar::hide);
        findReplaceBar.setOnQueryChanged(query -> scheduleFind(query, findReplaceBar.matchCase()));
        goToLineBar.setOnGo(this::goToLine);
        goToLineBar.setOnClose(goToLineBar::hide);
        VBox editorStack = new VBox(tabStrip.root(), findReplaceBar.root(), goToLineBar.root(), editor);
        VBox.setVgrow(editor, Priority.ALWAYS);
        workbench.setContent(RegionId.ACTIVITY, activityBar);
        workbench.setContent(RegionId.SIDE, sideBar);
        workbench.setContent(RegionId.EDITOR, editorStack);
        workbench.setContent(RegionId.INSPECTOR, new VBox(panel, room));
        workbench.setContent(RegionId.BOTTOM, bottomPanel);
        keymap.install(scene, workbench);
        editor.textProperty().addListener((obs, previous, text) -> {
            syncDocumentFromEditor(text);
            scheduleAnalysis(text);
            scheduleBracketMatch();
        });
        editor.caretPositionProperty().addListener((obs, previous, current) -> {
            panel.caretMoved(caretLine());
            updateCaretStatus();
            scheduleBracketMatch();
        });
        editor.setUndoManager(UndoUtils.richTextSuspendableUndoManager(editor, localUndo));
        editor.addEventFilter(KeyEvent.KEY_PRESSED, this::onEditorKeyPressed);
        updateCaretStatus();
        tabStrip.refresh();
    }

    Document document() {
        return document;
    }

    public EditorTabs editorTabs() {
        return editorTabs;
    }

    public TabStrip tabStrip() {
        return tabStrip;
    }

    public FindReplaceBar findReplaceBar() {
        return findReplaceBar;
    }

    public GoToLineBar goToLineBar() {
        return goToLineBar;
    }

    /** Inclusive indices of the highlighted bracket pair, or [-1, -1] when clear. */
    int[] bracketHighlight() {
        return new int[] {bracketOpen, bracketClose};
    }

    public SessionStore sessionStore() {
        return sessionStore;
    }

    /** Pinned tabs only; preview tabs are excluded from the session pin list. */
    public List<String> pinnedPaths() {
        return editorTabs.pinnedPaths();
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
        backgroundIo.execute(() -> {
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
        backgroundIo.shutdown();
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

    private void openFile(Path file, boolean pin) {
        if (file == null || projectRoot == null) {
            return;
        }
        String next = relative(file);
        if (!Document.isJavaPath(next)) {
            workbench.setStatus(Document.ONLY_JAVA_MESSAGE);
            return;
        }
        var existing = editorTabs.tabs().stream().filter(tab -> tab.relativePath().equals(next)).findFirst();
        if (existing.isPresent()) {
            if (pin) {
                existing.get().pin();
                persistSession();
            }
            editorTabs.selectPath(next);
            showActiveTab();
            onOpened.run();
            return;
        }
        int ticket = ++openGeneration;
        files.read(file, text -> Platform.runLater(() -> {
            if (ticket != openGeneration || text == null) {
                return;
            }
            Document opened = new Document(next, text);
            if (pin) {
                editorTabs.openPinned(opened);
            } else {
                editorTabs.openPreview(opened);
            }
            showActiveTab();
            persistSession();
            workbench.setStatus("");
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
            onRestored.run();
            return;
        }
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
        openFile(file, true);
    }

    private void persistSession() {
        Session session = new Session(
                projectRoot,
                editorTabs.pinnedPaths(),
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
            editorTabs.pinActive();
            tabStrip.refresh();
            persistSession();
        } catch (IllegalArgumentException failure) {
            workbench.setStatus(failure.getMessage());
        }
    }

    private void activateTab(int index) {
        editorTabs.select(index);
        showActiveTab();
    }

    private void showActiveTab() {
        OpenTab tab = editorTabs.active().orElse(null);
        if (tab == null) {
            document = null;
            syncingEditor = true;
            try {
                editor.clear();
            } finally {
                syncingEditor = false;
            }
            lastGood = null;
            panel.showUnparsed("");
            tabStrip.refresh();
            updateCaretStatus();
            return;
        }
        if (document == null || !document.relativePath().equals(tab.relativePath())) {
            lastGood = null;
            panel.showUnparsed("");
        }
        document = tab.document();
        syncingEditor = true;
        try {
            editor.replaceText(document.text());
        } finally {
            syncingEditor = false;
        }
        tabStrip.refresh();
        updateCaretStatus();
        scheduleAnalysis(document.text());
    }

    private void requestCloseTab(int index) {
        if (index < 0 || index >= editorTabs.tabs().size()) {
            return;
        }
        editorTabs.select(index);
        showActiveTab();
        closeActiveTab();
    }

    private void closeActiveTab() {
        OpenTab tab = editorTabs.active().orElse(null);
        if (tab == null) {
            return;
        }
        if (tab.document().dirty()) {
            SaveClosePrompt.Choice choice = saveClosePrompt.ask(tab.fileName(), scene.getWindow());
            if (choice == SaveClosePrompt.Choice.CANCEL) {
                return;
            }
            if (choice == SaveClosePrompt.Choice.SAVE) {
                saveActiveThenClose();
                return;
            }
        }
        editorTabs.closeActive();
        showActiveTab();
        persistSession();
    }

    private void saveActiveThenClose() {
        OpenTab tab = editorTabs.active().orElse(null);
        if (tab == null || projectRoot == null) {
            return;
        }
        String relative = tab.relativePath();
        String text = tab.document().text();
        String fileName = tab.fileName();
        Path file = projectRoot.resolve(relative);
        backgroundIo.execute(() -> {
            try {
                Files.writeString(file, text, StandardCharsets.UTF_8);
                Platform.runLater(() -> {
                    if (document != null && document.relativePath().equals(relative)) {
                        document.markSaved();
                    }
                    editorTabs.closeActive();
                    showActiveTab();
                    persistSession();
                });
            } catch (IOException failure) {
                Platform.runLater(() -> workbench.setStatus("Could not save " + fileName));
            }
        });
    }

    private void reopenClosedTab() {
        String path = editorTabs.reopen().orElse(null);
        if (path == null || projectRoot == null) {
            return;
        }
        openFile(projectRoot.resolve(path), true);
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
        workbench.register(new SimpleCommand(CommandId.CLOSE_TAB, "Close Tab", wb -> closeActiveTab()));
        workbench.register(new SimpleCommand(CommandId.REOPEN_TAB, "Reopen Closed Tab", wb -> reopenClosedTab()));
        workbench.register(new SimpleCommand(CommandId.FIND, "Find", wb -> findReplaceBar.show(false)));
        workbench.register(new SimpleCommand(CommandId.REPLACE, "Replace", wb -> findReplaceBar.show(true)));
        workbench.register(new SimpleCommand(CommandId.GO_TO_LINE, GoToLineBar.TITLE, wb -> goToLineBar.show()));
    }

    private void scheduleFind(String query, boolean matchCase) {
        String text = document == null ? "" : document.text();
        int ticket = ++findGeneration;
        backgroundIo.execute(() -> {
            List<BufferFinder.Match> matches = BufferFinder.findAll(text, query, matchCase);
            Platform.runLater(() -> {
                if (ticket != findGeneration) {
                    return;
                }
                findMatches = matches;
                findIndex = -1;
                if (query == null || query.isEmpty()) {
                    return;
                }
                if (matches.isEmpty()) {
                    workbench.setStatus(FindReplaceBar.NO_MATCHES);
                } else {
                    workbench.setStatus("");
                }
            });
        });
    }

    private void findNext() {
        if (findMatches.isEmpty()) {
            scheduleFindAndSelect(true);
            return;
        }
        int caret = editor.getCaretPosition();
        if (findIndex >= 0 && findIndex < findMatches.size()) {
            caret = findMatches.get(findIndex).end();
        }
        findIndex = BufferFinder.nextIndex(findMatches, caret);
        selectFindMatch();
    }

    private void findPrevious() {
        if (findMatches.isEmpty()) {
            scheduleFindAndSelect(false);
            return;
        }
        int caret = editor.getCaretPosition();
        if (findIndex >= 0 && findIndex < findMatches.size()) {
            caret = findMatches.get(findIndex).start();
        }
        findIndex = BufferFinder.previousIndex(findMatches, caret);
        selectFindMatch();
    }

    private void scheduleFindAndSelect(boolean forward) {
        String query = findReplaceBar.findField().getText();
        boolean matchCase = findReplaceBar.matchCase();
        String text = document == null ? "" : document.text();
        int ticket = ++findGeneration;
        int caret = editor.getCaretPosition();
        backgroundIo.execute(() -> {
            List<BufferFinder.Match> matches = BufferFinder.findAll(text, query, matchCase);
            Platform.runLater(() -> {
                if (ticket != findGeneration) {
                    return;
                }
                findMatches = matches;
                if (matches.isEmpty()) {
                    findIndex = -1;
                    if (query != null && !query.isEmpty()) {
                        workbench.setStatus(FindReplaceBar.NO_MATCHES);
                    }
                    return;
                }
                findIndex = forward
                        ? BufferFinder.nextIndex(matches, caret)
                        : BufferFinder.previousIndex(matches, caret);
                selectFindMatch();
            });
        });
    }

    private void selectFindMatch() {
        if (findIndex < 0 || findIndex >= findMatches.size()) {
            return;
        }
        BufferFinder.Match match = findMatches.get(findIndex);
        editor.selectRange(match.start(), match.end());
        workbench.setStatus("");
    }

    private void replaceCurrent() {
        if (document == null) {
            return;
        }
        String query = findReplaceBar.findField().getText();
        if (query == null || query.isEmpty()) {
            return;
        }
        String replacement = findReplaceBar.replaceField().getText();
        if (replacement == null) {
            replacement = "";
        }
        if (findIndex < 0 || findIndex >= findMatches.size()) {
            findNext();
            return;
        }
        BufferFinder.Match match = findMatches.get(findIndex);
        String selected = editor.getSelectedText();
        boolean same = findReplaceBar.matchCase()
                ? query.equals(selected)
                : query.equalsIgnoreCase(selected);
        if (!same) {
            findNext();
            return;
        }
        editor.replaceText(match.start(), match.end(), replacement);
        scheduleFind(query, findReplaceBar.matchCase());
        Platform.runLater(this::findNext);
    }

    private void replaceAllMatches() {
        if (document == null) {
            return;
        }
        String query = findReplaceBar.findField().getText();
        if (query == null || query.isEmpty()) {
            return;
        }
        String replacement = findReplaceBar.replaceField().getText();
        if (replacement == null) {
            replacement = "";
        }
        boolean matchCase = findReplaceBar.matchCase();
        String text = document.text();
        int ticket = ++findGeneration;
        String insert = replacement;
        backgroundIo.execute(() -> {
            List<BufferFinder.Match> matches = BufferFinder.findAll(text, query, matchCase);
            String updated = BufferFinder.replaceAll(text, query, insert, matchCase);
            Platform.runLater(() -> {
                if (ticket != findGeneration) {
                    return;
                }
                if (matches.isEmpty()) {
                    workbench.setStatus(FindReplaceBar.NO_MATCHES);
                    return;
                }
                // One buffer replace → one undo step.
                editor.replaceText(updated);
                findMatches = List.of();
                findIndex = -1;
                workbench.setStatus("Replaced " + matches.size());
            });
        });
    }

    private void goToLine(String input) {
        OptionalInt parsed = GoToLine.parse(input);
        if (parsed.isEmpty()) {
            return;
        }
        int lineCount = Math.max(1, editor.getParagraphs().size());
        GoToLine.Target target = GoToLine.clamp(parsed.getAsInt(), lineCount);
        editor.moveTo(target.line() - 1, 0);
        editor.requestFollowCaret();
        if (target.pastEnd()) {
            workbench.setStatus(GoToLine.pastEndMessage(target.requested()));
        } else {
            workbench.setStatus("");
        }
        goToLineBar.hide();
    }

    private void scheduleBracketMatch() {
        String text = editor.getText();
        int caret = editor.getCaretPosition();
        int ticket = ++bracketGeneration;
        // Windowed scan is ≤ 10k chars; keep it on the FX thread. Larger documents still
        // only examine the window around the caret (see BracketMatcher.DEFAULT_WINDOW).
        if (text.length() > BracketMatcher.DEFAULT_WINDOW * 2) {
            backgroundIo.execute(() -> {
                Optional<BracketMatcher.Match> match =
                        BracketMatcher.find(text, caret, BracketMatcher.DEFAULT_WINDOW);
                Platform.runLater(() -> {
                    if (ticket != bracketGeneration) {
                        return;
                    }
                    applyBracketHighlight(match);
                });
            });
            return;
        }
        applyBracketHighlight(BracketMatcher.find(text, caret, BracketMatcher.DEFAULT_WINDOW));
    }

    private void applyBracketHighlight(Optional<BracketMatcher.Match> match) {
        clearBracketHighlight();
        if (match.isEmpty()) {
            return;
        }
        BracketMatcher.Match found = match.get();
        editor.setStyleClass(found.open(), found.open() + 1, "bracket-match");
        editor.setStyleClass(found.close(), found.close() + 1, "bracket-match");
        bracketOpen = found.open();
        bracketClose = found.close();
    }

    private void clearBracketHighlight() {
        if (bracketOpen >= 0 && bracketOpen < editor.getLength()) {
            editor.clearStyle(bracketOpen, Math.min(bracketOpen + 1, editor.getLength()));
        }
        if (bracketClose >= 0 && bracketClose < editor.getLength()) {
            editor.clearStyle(bracketClose, Math.min(bracketClose + 1, editor.getLength()));
        }
        bracketOpen = -1;
        bracketClose = -1;
    }

    /** Local Enter only — remote newlines arrive via replaceText and skip this path. */
    private void onEditorKeyPressed(KeyEvent event) {
        if (event.getCode() != KeyCode.ENTER
                || event.isShiftDown()
                || event.isControlDown()
                || event.isAltDown()
                || event.isMetaDown()) {
            return;
        }
        int from = editor.getSelection().getStart();
        int to = editor.getSelection().getEnd();
        String text = editor.getText();
        int lineStart = from == 0 ? 0 : text.lastIndexOf('\n', from - 1) + 1;
        String lineBeforeCaret = text.substring(lineStart, from);
        editor.replaceText(from, to, AutoIndent.insertion(lineBeforeCaret));
        event.consume();
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
