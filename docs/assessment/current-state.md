# Current state assessment

Status: verified on 2026-10-14 against `5ddbabde` (`feat/ccmm-ide`) and the screenshot in `docs/assessment/baseline-screenshot.png`.

The screenshot draft treated the window as an empty shell. The code already has a Java editor, debounced metrics, and a collaboration text dump. Those features do not show up when the opened project is TypeScript, and the window is still not an IDE.

## What works

- Native JavaFX window (`Main`, `Launcher`). Installers are produced by the existing package workflow.
- File → Open Directory, or one directory argument, lists the project.
- Directory walking and file reads run on the daemon thread `ccmm-file-tree`, not the JavaFX thread. `FileNode.tree` throws if it is called on the JavaFX thread.
- A `.java` file opened by a single click is shown in a RichTextFX `CodeArea`. Typing does not analyze on the keystroke. `AnalysisScheduler` waits 300 ms, then calls `Analyzer.analyze` on `ccmm-analysis`. A newer edit drops the older result. `Platform.runLater` publishes it.
- For a Java file, `MetricsPanel` shows maintainability, cyclomatic complexity, code lines, Halstead volume, and loop nesting for the file and for the method at the caret. A later parse failure keeps the last good numbers and sets the status text to `Stale`.
- Remote text ops for the current file are applied in one batched edit. Undo is local. `ProjectRoomPanel` appends presence names and chat lines. The relay and protocol modules exist. The file tree is not sent on the socket.

## Class inventory (`ccmm-app` main)

| Class | Role |
|---|---|
| `Launcher` | Classpath entry that calls `Application.launch` |
| `Main` | Stage title `CCMM IDE`; opens `args[0]` when present |
| `IdeWindow` | Owns the scene, the path, the buffer, and the last analysis |
| `FileTreeController` | Single-thread executor `ccmm-file-tree` |
| `FileNode` | Eager recursive tree |
| `AnalysisScheduler` | 300 ms debounce, latest-wins |
| `MetricsPanel` | Right-hand labels, hidden until a Java parse succeeds |
| `ProjectRoom` / `ProjectRoomPanel` | Presence and chat as plain text. Initial body is `Here` |
| `RemoteEdits` | Applies accepted ops for the open relative path |

There is no document model, tab model, command registry, keymap, settings store, stylesheet, or WebSocket client.

## Observations, corrected

| # | Screenshot claim | Evidence | Severity |
|---|---|---|---|
| 1 | `page.tsx` is selected and the editor is blank | `IdeWindow` listens to the tree selection (single click, not double click) and `openFile` returns immediately unless the file name ends with `.java`. No empty state is shown. | High |
| 2 | Right panel says "Project Here" | `ProjectRoomPanel` title is `Project`. `ProjectRoom.text()` starts with `Here`. `MetricsPanel` hides its labels until a Java file parses, so a TypeScript project leaves only that placeholder. | Medium |
| 3 | The tree shows `.git`, `.next`, `.tmp`, `node_modules`, and build junk | `FileNode.read` lists every child, directories and files, with no ignore rules and no dimming. The walk is eager, not lazy: every directory is recursed before the tree is attached. | High |
| 4 | No icons, no git colour, minimal hover | The cell factory calls `setText` with the file name only. | Medium |
| 5 | Only a File menu | The menu is `Open Directory...` and nothing else. No command palette. | High |
| 6 | No tabs, breadcrumbs, status bar, or bottom panel | `BorderPane`: menu, tree, `CodeArea`, right `VBox`. | High |
| 7 | Live metrics are invisible | True on this screenshot, because no `.java` file was opened. The engine is invoked for Java buffers. The presentation is a label stack, not gutter chips, an outline, or a status bar. | High |
| 8 | Default Modena, light only | No application stylesheet. | Medium |
| 9 | A TypeScript project was opened | The editor refuses non-Java files. Metrics stay Java-only, which matches ADR-0004. Refusing to display the file does not. See proposed ADR-0010. | High |
| 10 | No collaboration entry point | The panel can render presence and chat once messages are handed to `applyRemote`. Nothing in the window connects a socket, shares an invite, or shows a caret. | High |

## Gap checklist

Still missing, beyond the Java editor and the label panel:

- **Editor core:** tabs and a dirty marker, highlighting, find/replace, go to line, bracket matching, auto-indent, folding, undo across files, encoding and line endings, large-file mode.
- **Navigation:** quick open, command palette, project search, outline, breadcrumbs, recents, back/forward, welcome.
- **Project:** create/rename/delete/move, reveal in the OS, a watcher, ignore rules, git status. The tree must stop loading ignored directories.
- **Panels:** problems, output, terminal, run/build. Terminal and run expand the approved non-goal "running code"; epic E6 is proposed so the owner can drop it.
- **Chrome:** status bar, themes, settings, keymap, session restore.
- **Metrics:** gutter chips, outline heat, inspector, dashboard, gate diagnostics. The label panel is the seed, not the UX.
- **Collaboration:** a share/join control, remote carets, follow, a real chat transcript, anchored threads, roles. Presence text and batched ops are the seed.

## Measurements

No coverage tool is configured. `rg @Test` on `*Test.java` finds 102 test methods across `ccmm-core`, `ccmm-cli`, `ccmm-collab-protocol`, `ccmm-collab-server`, and `ccmm-app`. `IdeWindowTest` assumes a display (`GraphicsEnvironment.isHeadless()`) and CI does not start one, so those tests skip on a headless Ubuntu runner.

Committed benchmarks, all marked `go` in their files:

| Benchmark | Recorded | Budget in the file |
|---|---|---|
| Cold start, 50 files | 995 ms | 2 s (`docs/benchmarks/cold-start.md`) |
| Keystroke p95, 10k-line `CodeArea` | 1.245 ms; committed memory 302.6 MB (spike row: 1.547 ms, 490.6 MB) | 16 ms (`docs/benchmarks/keystroke-latency.md`) |
| Metrics refresh from the last edit | 439 ms, quiet period 300 ms | the file says 300 ms and still records `go` (`docs/benchmarks/metrics-refresh.md`) |
| `Analyzer.scan`, 300 Java files | 499 ms; committed memory 360.9 MB | 3 s and 500 MB (`docs/benchmarks/project-scan.md`) |

`heliogrid` (`C:\Users\Bilal\Projects\heliogrid`), 2026-10-14, Windows 11 build 26200. A throwaway same-package caller invoked `FileNode.tree` and was not committed:

- 3,257 ms
- 31,847 nodes, 2,971 directories
- heap used 16.7 MB after the walk; heap committed 25.2 MB in that small JVM

That is the tree alone, not IDE startup and not the 500 MB budget. The walk already takes longer than the 3 s scan budget before the window can show a root. The IDE was not relaunched on this project; cold start remains the 50-file number above.

## Answers

1. **How does open-file work, and why is the editor blank?** Selection changes call `openFile`. A non-`.java` path returns before any read. `page.tsx` never reaches the `CodeArea`, and nothing tells the user why.
2. **Which thread loads the tree and reads files?** `ccmm-file-tree`. Analysis uses `ccmm-analysis`. The JavaFX thread only applies results and scene updates. The tree walk is off-thread and still eager.
3. **Is the metrics engine invoked?** Yes, for the open `.java` buffer, after 300 ms of quiet. It is not invoked for other files, and it is not invoked for the whole project from the IDE.
4. **What tests exist?** 102 `@Test` methods, listed by the search above. No coverage percentage is available.
5. **Startup and memory with `heliogrid` open?** Not measured as an IDE session. The eager tree build is 3,257 ms and 31,847 nodes. Use the committed cold-start and keystroke numbers for the IDE process.
6. **Is there a document model?** No. `IdeWindow` fields (`projectRoot`, `currentRelative`, `latest`, `lastGood`, `openGeneration`) and the `CodeArea` text are the state.

## Risks

- **Editor component.** ADR-0001 is accepted and the 10k-line p95 is under 16 ms. Folding, a 5 MB file, and gutter decorations were not measured. ADR-0008 stays proposed until epic E0.
- **UI architecture.** One `BorderPane` cannot host metrics decorations and remote carets. The workbench in `docs/architecture.md` is the seam.
- **Performance.** Ignoring `node_modules` is a correctness fix for the tree, not a polish item. 31,847 nodes on a real project is the evidence.
- **Scope.** Java-only metrics stay. Java-only opening does not; ADR-0010 asks the owner to change that locked line.
