# Design brief

Goal: turn "file tree + empty editor" into an IDE people want to keep open, where complexity metrics are ambient and collaboration is calm and purposeful.

The verified assessment is `docs/assessment/current-state.md`. This brief is the target. ADR-0013 records that current AtlantaFX requires a newer JDK than this repo, so the token file is the source of truth until the owner accepts a theme library.

## Principles

1. **Editor first.** The editor gets >= 65% of the window; chrome recedes (as in Zed and VS Code).
2. **Metrics are ambient.** Show them where the eye already is (gutter, outline, status bar); detail on demand.
3. **Keyboard first.** Everything reachable from the command palette; quick open; consistent shortcuts (Cmd on macOS, Ctrl elsewhere).
4. **Calm collaboration.** Presence is visible but never steals focus or viewport; interruptions are opt-in.
5. **Fast by default.** UI budgets in docs/PLAN.md; nothing blocks the FX thread.
6. **One visual system.** Tokens, not ad-hoc colours; dark default, light supported; contrast >= WCAG AA.

## Layout (default 1280x800, min 900x600)

```
+-----------------------------------------------------------------------------+
| menu bar                                  [avatars][Share]   (title bar zone)|
+---+-------------------+-----------------------------------------+-----------+
| A | Explorer          | [Foo.java *][Bar.java][page.tsx]        | Inspector |
| c | Search            | src > app > Foo > parse()  (breadcrumbs)|           |
| t | Outline           +-----------------------------------------+ Method    |
| i |  v src            | 12 | CC 7 | public Result parse(...) {  | parse()   |
| v |    Foo.java  ~    | 13 |      |   ...                       | CC 7  amb |
| i |    Bar.java       |    |      |                             | MI 64     |
| t | (git colours)     |    |      |                             | Halstead  |
| y |                   +-----------------------------------------+ trend     |
|   |                   | Problems | Metrics | Terminal | Chat     | gate rule |
+---+-------------------+-----------------------------------------+-----------+
| branch  LF  UTF-8  Ln 12, Col 5   CC 7  MI 64 (stale?)   3 online   Follow  |
+-----------------------------------------------------------------------------+
```

- **Activity bar:** Explorer, Search, Outline (complexity-sorted), Collaboration, Settings.
- **Inspector (right, collapsible):** replaces the "Project Here" placeholder; shows metrics for the method at the caret, rule status, trend. Hidden by default on narrow windows.
- **Bottom panel:** Problems (gate violations + parse errors), Metrics (project hotspots), Terminal, Chat. Collapsible.
- **Status bar:** position, encoding, EOL, branch, file metrics with a stale marker when the last parse failed, collaboration state.
- **Empty state / welcome:** recent projects, open folder, shortcuts, "Start a session".

## Metrics-first UX

- **Gutter chip** at each method signature: colour band (green/amber/red from `.ccmm.yml` thresholds) + CC number; click opens the Inspector.
- **Outline view** lists methods with a heat bar; sort by complexity; "hotspots only" filter.
- **Inspector:** CC with the list of decision points (click to jump), Halstead, MI, rule status, sparkline of the method's history from git.
- **Problems panel:** gate violations as diagnostics using the same engine as the CLI.
- **Project dashboard (editor tab):** worst 10 methods, file table with heat, delta vs base branch.
- **Pre-flight gate:** before commit/push, show which changed methods cross a threshold.
- **Stale handling:** on parse error keep the last good numbers, dim them, show "stale" in the status bar.

## Visual language

- Theme engine: evaluate AtlantaFX (CSS themes incl. Primer, Nord, Cupertino, Dracula) vs a custom stylesheet; either way define tokens in `docs/design/TOKENS.md` (surface 0–3, text primary/secondary/disabled, accent, success/warn/danger, selection, diff add/remove, 8 remote-user colours with AA contrast on both themes).
- Spacing scale 4/8/12/16/24; radius 4/6; row height 24 (tree), 28 (tabs); icon size 16.
- Editor font: bundled OFL monospace (agent to confirm licence) with ligatures optional; UI font: system.
- JavaFX limits font weights to regular/bold; design around that.
- Icons: pick one licensed set (agent to verify licence) via an icon-font/SVG loader; file-type icon map by extension.

## Editor component decision (mandatory spike)

Compare, with measurements on a 10k-line file and a 5 MB file: (a) RichTextFX `CodeArea`, (b) JavaFX incubating `CodeArea`/`RichTextArea` (requires a newer JavaFX), (c) any other viable option. Criteria: keystroke latency p95, scroll smoothness, memory, highlight cost, caret/selection API for remote carets, side decorations (gutter chips), folding feasibility, maintenance risk. Output: ADR + benchmark notes in docs/benchmarks/.

## Language scope (decision D5)

The user's own test project is TypeScript. Options: (1) editor is language-agnostic with highlighting for common languages, metrics Java-only in v1 (recommended); (2) Java-only files openable; (3) multi-language metrics in v1. For highlighting evaluate TextMate grammars (tm4e), per-language regex lexers, and tree-sitter bindings (check the JDK version they require).

## Reference survey (agent must re-verify and extend; paraphrase, never copy screenshots)

| Product | Borrow | Avoid | Source |
|---|---|---|---|
| VS Code | Workbench regions (activity bar, side bar, editor groups, panel, status bar), command palette, quick open | Extension-driven sprawl | VS Code UX docs |
| JetBrains IDEs | Structure/outline, gutter inspections, inline hints per method | Heavy chrome, modal dialogs | JetBrains docs |
| Zed | Minimal chrome, follow via avatars in the title bar, persistent channels, read-only guests | Collaboration that needs voice/sign-in to be useful | https://zed.dev/docs/collaboration/channels |
| VS Live Share | Host approval, read-only toggle, gitignored files hidden from guests by default, follow | Maintenance-mode feature set; shared terminals with write access by default | https://learn.microsoft.com/en-us/visualstudio/liveshare/use/share-project-join-session-visual-studio-code |
| JetBrains Code With Me | Lesson: a mainstream IDE is dropping pair programming (2026.1 last supported release, sunset Q1 2027) | — | https://blog.jetbrains.com/platform/2026/03/sunsetting-code-with-me/ |
