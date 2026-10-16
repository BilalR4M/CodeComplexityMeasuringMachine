# UI spec

Tokens live in [TOKENS.md](TOKENS.md). Wireframes live in [wireframes/](wireframes/). Shortcuts are Ctrl on Windows and Linux and Cmd on macOS; the tables write `Ctrl` and mean both. Minimum window 900×600. Dark and light are both required. Icon-only buttons expose an accessible name equal to the tooltip.

Shared states, used below only when they apply: empty, loading, populated, error, stale, disabled, reconnecting, remote-edit.

## Workbench shell

Wireframe: [wireframes/workbench.md](wireframes/workbench.md).

Regions, left to right: activity bar (48 px), side bar (default 260, min 160, max 480), editor (flex, at least 65% at 1280 with both side regions open), inspector (default 280, min 200). Bottom panel default 180, min 80. Status bar 24. Menu bar stays the platform menu.

- **Empty:** welcome fills the editor region. Side bar shows "No folder opened".
- **Loading:** status bar reads `Opening…`. The tree shows a single row `Loading…`.
- **Populated:** regions as the wireframe.
- **Error:** a failed open sets the status bar to `Could not open folder` and leaves the previous project in place.
- **Disabled:** inspector and bottom panel collapse; their commands stay enabled.

Interactions: drag the sash to resize; double-click a sash to collapse that region. `Ctrl+B` toggles the side bar. `Ctrl+J` toggles the bottom panel. `Ctrl+Alt+B` toggles the inspector. Below 1100 px the inspector opens collapsed. Focus order: activity bar, side bar, tabs, editor, inspector, bottom panel, status bar.

Copy: window title `CCMM IDE — <folder name>` or `CCMM IDE` when no folder is open.

## Explorer

Wireframe: [wireframes/explorer.md](wireframes/explorer.md).

Activity tooltip `Explorer (Ctrl+Shift+E)`. Side bar title `Explorer`.

- **Empty:** `No folder opened` and a button `Open Folder…`.
- **Loading:** `Loading…` until the first directory listing returns.
- **Populated:** directories first, then files, each by name. Ignored directories are absent (E4-T01). A git state, when known, colours the name: modified `warn`, added `success`, conflict `danger`, ignored hidden.
- **Error:** a directory that cannot be read shows the folder and the row `Could not read this folder`.

Single click selects. Single click on a file opens a preview tab. Double click or typing pins it. Arrow keys move. Right opens the context menu (E4-T08). No drag-and-drop in v1.

Copy: tooltip on a truncated name is the project-relative path.

## Tabs

Wireframe: [wireframes/tabs.md](wireframes/tabs.md).

- **Empty:** no tab strip; the welcome or the editor empty state shows instead.
- **Populated:** one tab per pinned document plus at most one italic preview tab. A dirty document shows a dot and `*` in the accessible name (`Foo.java, unsaved`).
- **Disabled:** a read-only viewer session still shows tabs; the editor is not editable.

Single click focuses. Middle click or `Ctrl+W` closes. `Ctrl+Shift+T` reopens the last of 20 closed tabs. `Ctrl+Tab` cycles. At overflow, tabs scroll and a chevron opens `Open tabs`. Closing a dirty tab asks `Save changes to <name>?` with `Save`, `Don't Save`, `Cancel`.

## Editor

Wireframe: [wireframes/editor.md](wireframes/editor.md).

- **Empty:** `Open a file from the explorer, or press Ctrl+P`.
- **Loading:** `Opening <name>…` until the read returns.
- **Populated:** text, line numbers, caret. Highlighting comes from E2-T03.
- **Error:** unreadable file: `Could not read <name>` and no buffer change. Binary or over the large-file threshold: large-file mode (E2-T09), no highlighting, banner `Large file — highlighting is off`.
- **Stale:** metrics decorations dim; the buffer stays editable.
- **Remote-edit:** remote carets and selections render; the viewport does not move unless follow is on.

`Ctrl+F` find, `Ctrl+H` replace, `Ctrl+G` go to line, `Ctrl+Z` / `Ctrl+Y` undo and redo (local only). The caret line and column feed the status bar. Typing schedules analysis only for `.java`.

## Quick open

Wireframe: [wireframes/quick-open.md](wireframes/quick-open.md).

`Ctrl+P`. A text field `Search files by name` over a list of project-relative paths. The index excludes ignored directories.

- **Empty query:** recent files, then `No recent files` if the list is empty.
- **Loading:** `Indexing…` while the first index builds. Typing stays responsive.
- **Populated:** up to 50 matches, directories dimmed with `text-secondary`.
- **Error:** `No matching files`.

Enter opens and pins. Escape closes and returns focus to the editor.

## Command palette

Wireframe: [wireframes/command-palette.md](wireframes/command-palette.md).

`Ctrl+Shift+P`. Field placeholder `Type a command`. Each row is the command title plus its shortcut.

- **Empty query:** recently used, then the full list.
- **Populated:** substring match on the title.
- **Error:** `No matching commands`.

Enter runs the command. Every command in E1-T06 appears here, including ones that have a shortcut.

## Search

Wireframe: [wireframes/search.md](wireframes/search.md).

Activity tooltip `Search (Ctrl+Shift+F)`. Fields: `Search`, `Replace` (hidden until the chevron), `Include`, `Exclude`. Default exclude is the ignore rules.

- **Empty:** `Type to search in <folder>`.
- **Loading:** `Searching…` with a count updating off the FX thread.
- **Populated:** file, then line, then a snippet. Cap 2,000 hits and say `Showing 2,000 matches. Narrow the query.`
- **Error:** `Nothing found`.

Enter on a hit opens the file at that line. Replace is explicit: `Replace` on one hit, `Replace All` with the confirm `Replace <n> matches in <folder>?`.

## Outline

Wireframe: [wireframes/outline.md](wireframes/outline.md).

Activity tooltip `Outline (Ctrl+Shift+O)`. For a Java file, methods with a heat bar (`success` / `warn` / `danger`) and the CC number. Sort control: `Position`, `Complexity`. Checkbox `Hotspots only`.

- **Empty:** non-Java file: `No outline for this file`. No document: `Open a file to see its outline`.
- **Loading:** `Analyzing…` until the first result.
- **Stale:** the list stays, dimmed, with `Outline is stale`.
- **Populated:** click a row to move the caret to `startLine`.

## Inspector

Wireframe: [wireframes/inspector.md](wireframes/inspector.md).

Title `Inspector`. Hidden until a method is selected or a gutter chip is clicked. Collapsed by default under 1100 px.

- **Empty:** `Click a complexity chip or a method in the outline`.
- **Populated:** method name, CC, the decision-point list (each row jumps), MI, Halstead volume, nesting, rule line `Passes` or `Breaks <rule>`, and `History` once git data exists.
- **Stale:** banner `These numbers are stale` and the last good values dimmed.
- **Error:** never-parsed buffer: `This file has not parsed` plus the parser message. No invented numbers.

Copy for a missing method: `Caret is outside a method`.

## Problems

Wireframe: [wireframes/problems.md](wireframes/problems.md).

Bottom tab `Problems`. Rows are parse failures and gate violations from the same rules as `ccmm gate`.

- **Empty:** `No problems`.
- **Loading:** `Checking…`.
- **Populated:** severity icon, message, `path:line`. Click jumps.
- **Error:** missing `.ccmm.yml` on an explicit check: `No .ccmm.yml — using the default thresholds` once, not a failure dialog.

Copy examples: `Cyclomatic complexity 14 exceeds 10`, `Could not parse: <message>`.

## Metrics dashboard

Wireframe: [wireframes/dashboard.md](wireframes/dashboard.md).

Opened as an editor tab titled `Metrics`. Command `Open Metrics Dashboard`.

- **Empty:** `Open a folder to see project metrics`.
- **Loading:** `Scanning Java files…`.
- **Populated:** `Worst methods` (10 rows: method, file, CC, MI) and a file table with a heat bar. `Delta vs <branch>` when a base branch is set.
- **Error:** `Scan failed: <message>` and the previous table if one exists.

Non-Java files are absent from the table.

## Status bar

Wireframe: [wireframes/status-bar.md](wireframes/status-bar.md).

Left: branch or `no git`, then EOL (`LF` or `CRLF`), encoding (`UTF-8` or the detected name). Right: `Ln <n>, Col <n>`, then `CC <n>` and `MI <n>` when the caret is in a method, then `stale` when the last parse failed, then `<n> online` when a session is connected.

- **Empty:** `Ln 1, Col 1` only.
- **Stale:** the word `stale` in `warn`, tooltip `Showing the last successful analysis`.
- **Reconnecting:** `Reconnecting…` replaces `<n> online`.

Click branch for a menu that only copies the name in v1. Click CC to focus the inspector.

## Welcome

Wireframe: [wireframes/welcome.md](wireframes/welcome.md).

Shown when no folder is open and when the user runs `Show Welcome`.

- **Empty:** no recents: heading `CCMM IDE`, buttons `Open Folder…` and `Start a Session…`, three shortcut lines.
- **Populated:** `Recent` with up to 10 projects. A missing path shows `Folder missing` and does not crash.

Copy: `Open Folder…`, `Start a Session…`, `Ctrl+P Quick open`, `Ctrl+Shift+P Commands`, `Ctrl+Shift+F Search`.

## Settings

Wireframe: [wireframes/settings.md](wireframes/settings.md).

Activity tooltip `Settings (Ctrl+,)`.

Fields: `Theme` (`Dark`, `Light`), `Editor font size` (8–32), `Ligatures` (`Off`, `On`), `Relay URL`, `Display name`. Stored in the OS user config directory (E1-T09).

- **Error:** a rejected relay URL shows `Enter a ws:// or wss:// URL` under the field. The previous value stays saved.

`Reset to defaults` asks `Reset all settings?` with `Reset` and `Cancel`.

## Collaboration panel

Wireframe: [wireframes/collaboration.md](wireframes/collaboration.md).

Activity tooltip `Collaboration`. Title `Session`.

- **Empty:** `Not sharing`. Buttons `Start session` and `Join session`.
- **Loading:** `Connecting…`.
- **Populated:** mode (`Pair`, `Review`, `Teach`), role, people rows (swatch, name, file or `idle`), `End session` for the host.
- **Error:** `Could not connect` and `Wrong or expired invite`.
- **Reconnecting:** `Reconnecting…` and `n edits buffered`.

People rows do not steal focus. Click a row to follow (E7-T04).

## Follow chip

Wireframe: [wireframes/follow.md](wireframes/follow.md).

A single chip over the editor, top centre: `Following <name>` and `Stop`. Tooltip `You are following <name>. Click Stop, or move the caret, to follow on your own.`

- **Populated:** visible only while following.
- **Disabled:** hidden.

Any local caret move, file open, or scroll from the user hides it and stops follow. Remote edits do not.

## Threads

Wireframe: [wireframes/threads.md](wireframes/threads.md).

Collaboration view section `Threads`, and a gutter mark on the anchor line.

- **Empty:** `No comments yet`.
- **Populated:** each row is the first line, the author, `Open` or `Resolved`. Click jumps.
- **Error:** a deleted range reads `Outdated — the code was removed`.

Composer: `Comment on this range`, button `Comment`. Host and viewers may comment. A viewer cannot edit the file.

## Session summary

Wireframe: [wireframes/summary.md](wireframes/summary.md).

Command `Export Session Summary`. A preview tab `Session summary` with Markdown: files touched, metric deltas, open threads, resolved threads. Buttons `Copy` and `Save…`.

- **Empty:** `Nothing to export yet`.
- **Error:** `Could not write the file`.

The preview is local. It is not sent to the relay.
