# Issue specification

An agent must be able to finish any issue from its text alone, plus the docs it links. Vague issues are rejected in review. One issue = one PR.

Labels: `agent-task`, `epic:<name>`, `size:S|M|L`, `kind:code|ui|spike|docs`. Title: `<ID> <verb> <thing>` (ID format `E<epic>-T<nn>`).

## Code issue template

```
## Context        why this exists; link docs/PLAN.md ID, ADRs, design-brief section
## Goal           one sentence, user-visible outcome
## Scope / Out of scope
## Design
- Modules, packages, classes; public signatures (Java) for new types
- Threading: which thread/executor, cancellation, debounce
- Data flow: step list or sequence (edit -> event -> handler -> result)
- Errors and edge cases: explicit list (empty file, huge file, parse error, IO failure, Windows paths)
- Config/persistence: keys, file locations (per-OS user config dir)
## Implementation steps   ordered; each independently committable
## Acceptance criteria    Given/When/Then, testable
## Tests                  named test classes; unit/property/integration; benchmarks if perf-relevant
## Verification           exact commands, expected output, numbers vs budget
## Depends on / Blocks
## Risks and fallbacks
```

## UI issue template (adds to the above)

```
## UX spec
- Wireframe: ASCII inline or docs/design/wireframes/<id>.png
- States: empty, loading, populated, error, stale, disabled, reconnecting, remote-edit (as applicable)
- Interactions: mouse, keyboard (Ctrl/Cmd variants), drag, context menu, focus order
- Visuals: tokens from docs/design/TOKENS.md only (no raw hex), icons, density, min size 900x600, HiDPI 100/150/200%
- Copy: exact labels, tooltips, empty-state text, error messages
- Accessibility: keyboard reachability, accessible text on icon-only buttons, contrast AA, minimum hit target
## Evidence required in the PR
Screenshots: dark + light, 100% + 150% scale; a short GIF for interactions; FX-thread latency note for anything that updates on keystrokes.
```

## Spike issue template

```
## Question        the decision this spike informs (links the ADR)
## Method          what to build/measure, dataset, hardware note
## Metrics         what is recorded and where (docs/benchmarks/<id>.md)
## Go / no-go      explicit thresholds and the plan change a no-go triggers
## Output          ADR update + benchmark notes; spike code lives under spikes/<id>/ and is not shipped
```

## Granularity rules

- Split UI shell, behaviour, and persistence into separate issues when each is >= M.
- A feature that touches both the engine and the UI becomes two issues with a dependency.
- No issue above size L (~400 lines). Include test and benchmark work inside the issue, not as follow-ups.

## Worked example (UI): E2-T04 Tab strip with dirty marker

```
Context: docs/design/DESIGN_BRIEF.md "Layout"; depends on E1-T02 (workbench regions), E2-T01 (document model).
Goal: open files appear as tabs; unsaved changes are visible; tabs are closable by mouse and keyboard.
Design: `TabStrip` (ccmm-app/.../ui/editor) wraps `TabPane`; `DocumentModel.dirtyProperty()` drives a dot via a CSS pseudo-class `:dirty`; single click opens a preview tab (italic) that is replaced by the next single click; double click or edit pins it; middle-click and Ctrl/Cmd+W close; reopen with Ctrl/Cmd+Shift+T (stack of last 20).
Acceptance:
- Given a file opened by single click, when another file is single-clicked, then the preview tab is replaced.
- Given a dirty tab, when closed, then a Save/Discard/Cancel prompt appears.
- Given 40 open tabs, when the window is narrow, then tabs scroll and a dropdown lists all.
Tests: TabStripTest (headless), DocumentModelDirtyTest.
Evidence: screenshots dark/light at 100%/150%; GIF of preview-replace.
```
