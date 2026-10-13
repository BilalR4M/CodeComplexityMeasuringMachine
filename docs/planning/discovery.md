# Discovery

Read from git tag `legacy-web-v1` (worktree `../ccmm-legacy`) and from [fixtures/legacy/sampleCode.txt](../../fixtures/legacy/sampleCode.txt). This note records what the web app did. It is not the specification for the desktop IDE.

## What the legacy app is

Two programs:

- `CCMM/` is a React client and a Socket.IO server. Routes are a home page, four calculator pages, and `/editor/:roomId`.
- `CCMM-Metrics-API/` is a Spring Boot 3.1 app. `pom.xml` sets Java 17 and the compiler plugin sets source/target 19. The only live endpoints are the four metric calculators below. `Report` is an empty class. `RecursionReport` is a data holder with no endpoint. `DatabaseConnection.java` is entirely commented out and contains a database URI; do not copy that file forward. The URI stays only in the legacy tag history.

The client pages under `client/src/test/` are copies of the calculator screens, not an automated suite. The API test is the generated Spring context test.

## Feature inventory

| Surface | Behaviour |
|---|---|
| `/halsteadmetriccalc` | Posts plain text to `POST /calculate-halstead`. Shows vocabulary, length, volume, difficulty, effort. Can save a PDF. |
| `/loccal` | Posts `{code}` to `POST /calculate-line-count`. Shows code, comment, blank, and a method count. Can save a PDF. |
| `/maintainabilitycal` | Posts plain text to `POST /calculate-maintainability-index`. Shows rounded MI, cyclomatic complexity, and LOC. |
| `/complexitycal` | Posts `{code}` to `POST /calculate-loop-depth`. Shows one integer. Can save a PDF. |
| Editor `/editor/:roomId` | CodeMirror editor, file tree, tabs, settings, run-code panel, drawing sync, chat, presence. The complexity sidebar posts the active file to the MI endpoint and the loop-depth endpoint on every content change and copies the JSON to the clipboard. |
| Socket.IO server | One room per `roomId`. Syncs the whole file tree (create, rename, delete, update), chat, typing, presence, and drawing strokes. Usernames must be unique in the room. No authentication. |

[fixtures/legacy/sampleCode.txt](../../fixtures/legacy/sampleCode.txt) is a single Java class, `TwoMethodsWithNestedLoop`, with `main`, `sum`, `printMultiplicationTable` (a `for` inside a `for`), and `isEven`. It is the only checked-in sample.

## Metric formulas

### Halstead (`HalsteadMetricsCalculator`)

Regex over each line after stripping `//` comments and a single-line `/* */`. Unique operators are the regex source strings that matched, not the text that matched, so `+` and `++` can both count and overlap. Operands are identifiers only. Numeric literals are ignored. Java keywords are excluded. Volume, difficulty, and effort use the usual Halstead equations (`log` base 2). An earlier copy of the class is left commented out at the bottom of the file and does not exclude keywords.

### Lines (`LineCountCalculator`)

Counts blank lines, `//` lines, and `/* */` lines, including a block-comment flag. Every other line is code, including lines that are only a brace. Method count is a separate scan: a line that has both a modifier or primitive type word and a `(` is treated as a method. A signature split across lines is remembered until `{`. The word lists are Java-oriented, but the scan is not a parser.

### Maintainability index (`MaintainabilityIndexCalculatorController`)

Returns `double[]` of `{rounded MI, cyclomatic complexity, non-empty line count}`.

- Cyclomatic complexity starts at 1 and adds 1 for each match of `if`, `else`, `for`, `while`, `do`, `switch`, `case`, `catch`, `throw`, `return`, `&&`, and `||`.
- LOC is the count of non-empty lines, not the line-count endpoint.
- Halstead volume uses a third operator list (it includes `?` and `:`) and treats every `\w+` token as an operand, including keywords. This is not the Halstead endpoint.

MI is `(171 - 5.2 * ln(volume) - 0.23 * cyclomatic - 16.2 * ln(loc)) * 100 / 171`, then clamped at 0 and rounded. The editor paints 0–9 red, 10–19 yellow, and 20–100 green.

### Loop nesting (`NestingDepthCalculator`)

Strips modifiers and comments, then walks characters. Depth increments when the remaining text starts with `for`, `while`, or `do`, so `double` counts as `do`. Depth decrements on every `}`. The maximum depth is the result. Braces that belong to classes, methods, or `if` statements change the depth.

## Parity gaps

- v1 analysis is Java-only, through JavaParser. The legacy calculators accept any text.
- v1 metrics are an in-process library. The legacy editor needs `localhost:8080`.
- v1 does not sync the file tree, run arbitrary languages, or share a drawing canvas. The legacy editor does all three.
- v1 has a CLI quality gate and a report. The legacy app has neither.
- PDF export and clipboard copy exist in the legacy UI. They are not a v1 requirement.
- Two Halstead implementations and two LOC definitions disagree with each other. Copying either regex into `ccmm-core` would freeze those bugs. ADR-0004 proposes JavaParser formulas and keeps the legacy sample only as an input fixture.
- There is no automated golden output for `sampleCode.txt`. Expected desktop numbers will be produced by the Phase 1 tests, not by replaying the regex.

## Unknowns

- RichTextFX keystroke latency on a 10k-line file is unmeasured (Phase 0).
- Whether server-authoritative OT converges for the edits we care about is unmeasured (Phase 0). ADR-0002 is still proposed.
- How the owner wants invite tokens issued is unresolved (ADR-0006).
- Signing secrets for installers are not configured. Package-check already built unsigned installers on four runners.

## Risks

- Treating legacy regex output as the oracle would make the new engine wrong in the same ways (`double` nesting, overlapping operators, `else` counted as a branch).
- The legacy tag contains a database URI inside a comment. A later copy of that file would publish it again.
- Phase 4 cannot start while ADR-0002 is proposed. Building the relay early would guess the algorithm, the hosting model, and the auth model.
- JavaFX tests need a display. The keystroke spike must record a local measurement; CI is not a substitute for that number.
