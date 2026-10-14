# Planning protocol: IDE-grade UX re-plan

Extends docs/agents/PLANNING.md. Mission: take the build from "file tree + empty editor" to a credible IDE with ambient live metrics and purposeful collaboration. Planning only: no production code in the planning PR.

Task ids for this program are `E<epic>-T<nn>`. Do not reuse a `P*` id. Completed Phase 0–4 stays in `docs/PLAN.md` as history.

## Inputs (read all first)

1. AGENTS.md, docs/agents/*, docs/adr/*, docs/PLAN.md (if present).
2. docs/assessment/current-state.md and the baseline screenshot (the assessment is verified; do not treat the screenshot as the only source).
3. docs/design/DESIGN_BRIEF.md, docs/design/COLLABORATION.md, docs/design/UI_SPEC.md, docs/design/TOKENS.md.
4. The code in `ccmm-app` and `ccmm-core` as it exists, plus `git worktree add ../ccmm-legacy legacy-web-v1` when a metric formula is in question.

## Required outputs

1. **Verified assessment** — `docs/assessment/current-state.md` with class inventory, threading, tests, and measurements. Correct the screenshot draft in place.
2. **Reference survey** — `docs/design/REFERENCE_SURVEY.md`. Dated source links only. No copied screenshots or text.
3. **Design system** — `docs/design/TOKENS.md` and `docs/design/UI_SPEC.md`. Wireframes in `docs/design/wireframes/`.
4. **ADRs (proposed, not accepted)** — editor component, highlighting, language scope D5, collaboration ownership, JDK/JavaFX, theming, icon/font licences, UI test strategy.
5. **docs/architecture.md** — workbench model and the decoration API.
6. **Epics and issues** — every issue follows `docs/agents/ISSUE_SPEC.md`. Bodies live in `docs/planning/issues/` and on GitHub.
7. **docs/PLAN.md** — `Status: PROPOSED` until the owner approves. Keep the completed `P*` phases. New work uses `E*` ids. Append the change log; do not rewrite it.

## Epic spine

| Epic | Scope |
|---|---|
| E0 Spikes | Editor-component bake-off; highlighting engine; anchor stability under OT; theming + HiDPI; headless UI testing on CI |
| E1 Workbench shell | Region layout, activity bar, resizable/collapsible panels, status bar, command registry, keymap, theme tokens, settings store, session restore |
| E2 Editor core | Document model, tabs (preview/pinned/dirty), highlighting, find/replace, go to line, bracket match, auto-indent, EOL/encoding, large-file mode, folding if the spike says it is feasible |
| E3 Navigation | Quick open, command palette, global search (ignore-aware), outline, breadcrumbs, back/forward, recent files and projects, welcome screen |
| E4 Project and files | Ignore-aware lazy tree, file icons, git status decorations, new/rename/delete/move, reveal in OS, watcher refresh (poll fallback on macOS), context menus |
| E5 Live metrics UX | Gutter chips, outline heat, inspector, status-bar metrics, stale handling on those surfaces, Problems (same engine as the CLI), project dashboard, pre-flight gate |
| E6 Run and terminal | Build/run tasks, output panel, embedded terminal. Expands the v1 non-goal "running code". The owner may delete this epic. |
| E7 Collaboration | Share/join, handshake, carets, follow, chat, presence in tree/tabs, anchored threads, metrics lens, session summary, roles, guest file hiding. Extends Phase 4; does not replace the relay. |
| E8 Polish | Accessibility, HiDPI/perf against budgets, empty/error copy, onboarding |
| E9 Release | Signing and notarization notes, update check, crash/log location. No edits to the release workflow in the planning PR. |

Rules: spikes gate the epics they inform. E4 does not wait on the editor bake-off. E7 starts after the document model and tabs exist; its anchor spike runs in E0. Metrics UX (E5) lands before the collaboration metrics lens.

## Issue creation

```
gh label create "epic:<name>" --force
gh label create "kind:ui" --force
gh issue create --title "<ID> <title>" --label "agent-task,epic:<name>,size:<s>,kind:<k>" --body-file <file>
```

Write each issue body to `docs/planning/issues/<ID>.md` first. Put the GitHub issue number back into `docs/PLAN.md`. Prefer more, smaller issues. None above size L.

## Quality bar

- An agent with no prior context can complete any issue from its text and the linked docs without asking questions.
- Every UI issue has a wireframe, the states that apply, shortcuts, copy, accessibility notes, and required screenshots.
- Every metric or perf claim names the benchmark that proves it.
- The plan PR is docs-only. Set `Status: PROPOSED` and stop for owner review.
