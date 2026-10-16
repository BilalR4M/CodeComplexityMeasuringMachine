# PLAN
Status: APPROVED
Version: 3

The owner sets `Status: APPROVED` and adds the `plan-approved` label. No implementation starts before that.

## Goals

- A JavaFX IDE that shows live Java complexity metrics from `ccmm-core`.
- A CLI, `analyze | gate | report`, using that same engine for local runs and pull-request gates.
- Collaboration (co-editing, chat, presence) after ADR-0002, ADR-0005, and ADR-0006 are accepted.

## Non-goals

- Copying the legacy regex numbers. See ADR-0004.
- Language servers, non-Java metrics, cognitive complexity (ADR-0007), running code, a drawing canvas, PDF export, and syncing the file tree.
- A hosted multi-tenant relay. Operators run the published image (ADR-0005).

## Budgets

- Cold start to a usable window is under 2 s. Benchmark: `docs/benchmarks/cold-start.md` (P3-T01).
- Keystroke latency p95 on a 10k-line file is under 16 ms. Benchmark: `docs/benchmarks/keystroke-latency.md` (P0-T01, checked again in P3-T02).
- Live metrics refresh about 300 ms after typing stops. Benchmark: `docs/benchmarks/metrics-refresh.md` (P3-T03).
- A few-hundred-file project stays under 500 MB. Benchmark: `docs/benchmarks/keystroke-latency.md` records editor memory; `docs/benchmarks/project-scan.md` records scan memory (P0-T01, P1-T08).
- Initial project scan is under 3 s. Benchmark: `docs/benchmarks/project-scan.md` (P1-T08).

## Risks

- RichTextFX misses the keystroke budget. Likelihood medium. Impact high. Mitigation: P0-T01 go/no-go, which blocks P3-T02 and names a replacement editor. Owner: the P0-T01 assignee.
- OT does not converge. Likelihood medium. Impact high. Mitigation: P0-T02 go/no-go replaces ADR-0002 instead of starting Phase 4. Owner: the P0-T02 assignee.
- Callers expect legacy regex numbers. Likelihood high. Impact medium. Mitigation: ADR-0004 and golden fixtures that lock the JavaParser numbers. Owner: whoever accepts ADR-0004.
- The legacy tag contains a database URI inside a comment. Likelihood low if nobody copies `DatabaseConnection.java`. Impact high. Mitigation: discovery forbids copying that file. Owner: reviewer of any legacy port.
- Phase 4 starts while the ADRs are still proposed. Likelihood medium. Impact high. Mitigation: Phase 4 issues depend on the open #3. Owner: the person who accepts the ADRs.
- The file tree walks ignored directories. Likelihood high. Impact high. The `heliogrid` checkout built 31,847 nodes in 3,257 ms. Mitigation: E4-T01 and E4-T02. Owner: the E4-T02 assignee.
- The editor bake-off no-gos RichTextFX. Likelihood medium. Impact high. Mitigation: E0-T01 names the replacement and blocks the E2 tasks that assume RichTextFX. Owner: the E0-T01 assignee.
- E6 is built while the owner still treats running code as a non-goal. Likelihood medium. Impact medium. Mitigation: E6 is marked proposed and can be deleted without touching E0–E5 or E7. Owner: the person who accepts this plan.

## Open decisions

Accepted and unchanged:

- [ADR-0002](adr/0002-collaboration-algorithm.md) operational transform, accepted.
- [ADR-0004](adr/0004-metric-formulas.md) JavaParser formulas, accepted.
- [ADR-0005](adr/0005-relay-hosting.md) operator-run image, accepted.
- [ADR-0006](adr/0006-room-auth.md) invite token, accepted.
- [ADR-0007](adr/0007-cognitive-complexity.md) post-v1, accepted.

Proposed for the UX program. None of these are accepted. Implementation issues that name one of them stay blocked until the owner accepts it.

- [ADR-0008](adr/0008-editor-component.md) editor component. The bake-off decides. ADR-0001 stays accepted until a replacement is accepted.
- [ADR-0009](adr/0009-highlighting-engine.md) highlighting engine.
- [ADR-0010](adr/0010-language-scope.md) language scope. Recommends opening any text file and keeping Java-only metrics. This asks the owner to change the locked "Java-only language support" line. No language server.
- [ADR-0011](adr/0011-collaboration-ownership.md) local checkouts plus a hash handshake. The file tree stays unsynced.
- [ADR-0012](adr/0012-jdk-javafx-version.md) stay on Java 21 and JavaFX 21.0.5.
- [ADR-0013](adr/0013-theming.md) token stylesheets. Current AtlantaFX needs a newer JDK, so it is not adopted.
- [ADR-0014](adr/0014-icons-and-font.md) JetBrains Mono (OFL) and Lucide, including the Feather MIT notice.
- [ADR-0015](adr/0015-ui-test-strategy.md) headless UI tests on CI. The workflow is not edited in this plan.

E6 (tasks, output, terminal) expands the non-goal "running code". The owner may delete E6 and its issues. Until then the non-goal stays, and E6 is proposed work rather than a silent reversal.

## Phases

### Phase 0 — Spikes

Goal: prove the two decisions that can invalidate this plan. Exit criteria: `docs/benchmarks/keystroke-latency.md` and `docs/benchmarks/ot-convergence.md` each end with `go` or `no-go`, and a no-go names the plan change.

#### P0-T01 RichTextFX keystroke spike
- [x] P0-T01 RichTextFX keystroke spike
      why:        ADR-0001 requires proof before the editor is built.
      scope:      `ccmm-app` harness and `docs/benchmarks/keystroke-latency.md`
      depends on: none
      acceptance:
      - A 10,000-line Java buffer is loaded into a RichTextFX `CodeArea`.
      - The benchmark file records keystroke latency p95, process memory, the OS, and the command that produced the numbers.
      - The file ends with `go` when p95 is under 16 ms, otherwise `no-go`.
      - A no-go states that P3-T02 is replaced with an editor change and does not implement the editor.
      verify:     `docs/benchmarks/keystroke-latency.md` contains p95, memory, and `go` or `no-go`. `mvn -B -ntp -pl ccmm-app -am test`
      size:       M
      issue:      #4
      pr:         #28

#### P0-T02 OT convergence spike
- [x] P0-T02 OT convergence spike
      why:        Phase 4 stays blocked until concurrent edits converge or ADR-0002 is replaced.
      scope:      `ccmm-collab-protocol/src/test` and `docs/benchmarks/ot-convergence.md`
      depends on: none
      acceptance:
      - Tests cover two clients applying concurrent inserts and concurrent deletes to the same plain text.
      - Both clients finish on one string. The server order is the authority.
      - No relay process and no JavaFX code.
      - The benchmark file ends with `go` or `no-go`. A no-go says ADR-0002 must be replaced and does not start Phase 4.
      verify:     `mvn -B -ntp -pl ccmm-collab-protocol test`
      size:       M
      issue:      #5
      pr:         #29

### Phase 1 — Metrics engine

Goal: `ccmm-core` reports the ADR-0004 metrics for one file and for a directory. Exit criteria: `mvn -B -ntp -pl ccmm-core test` passes, including the golden fixture.

#### P1-T01 Analysis result and parse
- [x] P1-T01 Analysis result and parse
      why:        Every later metric needs one parse result that cannot throw a syntax error at the UI.
      scope:      `ccmm-core`
      depends on: none
      acceptance:
      - `Analyzer.analyze` returns `Ok` or `Failed` as in `docs/architecture.md`.
      - A syntax error returns `Failed` with the parser message and does not throw.
      - Tests cover one valid compilation unit and one invalid buffer.
      verify:     `mvn -B -ntp -pl ccmm-core test`
      size:       M
      issue:      #6
      pr:         #30

#### P1-T02 Line counts
- [x] P1-T02 Line counts
      why:        LOC feeds maintainability index and the panel, and the legacy endpoint disagrees with the MI scan.
      scope:      `ccmm-core`
      depends on: P1-T01
      acceptance:
      - The result includes physical, code, comment, and blank lines.
      - A comment-only line and a blank line are not code.
      - A block comment that spans lines counts each of those lines as a comment.
      verify:     `mvn -B -ntp -pl ccmm-core test`
      size:       S
      issue:      #7
      pr:         #32

#### P1-T03 Cyclomatic complexity
- [x] P1-T03 Cyclomatic complexity
      why:        The legacy counter treats `else`, `return`, and `throw` as branches.
      scope:      `ccmm-core`
      depends on: P1-T01
      acceptance:
      - Per method, McCabe starts at 1 and adds 1 for `if`, `for`, `while`, `do`, `case`, `catch`, conditional `&&`, conditional `||`, and a ternary.
      - `else`, `return`, `throw`, and `switch` do not add.
      - The file value is the sum of its methods.
      verify:     `mvn -B -ntp -pl ccmm-core test`
      size:       M
      issue:      #8
      pr:         #34

#### P1-T04 Halstead metrics
- [x] P1-T04 Halstead metrics
      why:        The legacy app has two different Halstead scans. Maintainability index must use this one.
      scope:      `ccmm-core`
      depends on: P1-T01
      acceptance:
      - Operators and operands come from the AST. Keywords are not operands. Numeric and string literals are operands.
      - The result includes n1, n2, N1, N2, vocabulary, length, volume, difficulty, and effort, with log base 2.
      - File-level and method-level values are both present.
      verify:     `mvn -B -ntp -pl ccmm-core test`
      size:       M
      issue:      #9
      pr:         #35

#### P1-T05 Maintainability index
- [x] P1-T05 Maintainability index
      why:        The editor's main number must use the same volume, complexity, and LOC as the other metrics.
      scope:      `ccmm-core`
      depends on: P1-T02, P1-T03, P1-T04
      acceptance:
      - MI uses `(171 - 5.2 * ln(volume) - 0.23 * cyclomatic - 16.2 * ln(codeLines)) * 100 / 171`.
      - The value is clamped at 0.
      - There is no second operator scan.
      verify:     `mvn -B -ntp -pl ccmm-core test`
      size:       S
      issue:      #10
      pr:         #37

#### P1-T06 Loop nesting
- [x] P1-T06 Loop nesting
      why:        The legacy depth counter treats `double` as a `do` loop.
      scope:      `ccmm-core`
      depends on: P1-T01
      acceptance:
      - Depth is the maximum AST nesting of `for`, `while`, and `do`.
      - An identifier that merely starts with those letters does not count.
      - `printMultiplicationTable` in the legacy sample is depth 2.
      verify:     `mvn -B -ntp -pl ccmm-core test`
      size:       S
      issue:      #11
      pr:         #36

#### P1-T07 Golden fixtures
- [x] P1-T07 Golden fixtures
      why:        Metric changes have to be deliberate, and the legacy sample is the shared input.
      scope:      `ccmm-core` and `fixtures/golden/`
      depends on: P1-T02, P1-T03, P1-T04, P1-T05, P1-T06
      acceptance:
      - `fixtures/golden/` includes the text of `fixtures/legacy/sampleCode.txt` and the expected file and per-method metrics.
      - A test fails when the engine drifts from that expected file.
      - The expected file is the ADR-0004 result, not the legacy regex result. The fixture README says so.
      verify:     `mvn -B -ntp -pl ccmm-core test`
      size:       M
      issue:      #12
      pr:         #38

#### P1-T08 Project scan
- [x] P1-T08 Project scan
      why:        The CLI and the IDE open directories, not single pasted buffers.
      scope:      `ccmm-core` and `docs/benchmarks/project-scan.md`
      depends on: P1-T01, P1-T07
      acceptance:
      - `Analyzer.scan` returns one result per `*.java` file under the root.
      - Files larger than 1 MiB, and symlinks that leave the root, are skipped and named.
      - A few-hundred-file tree is timed. `docs/benchmarks/project-scan.md` records the duration against the 3 s budget and the memory against 500 MB.
      verify:     `mvn -B -ntp -pl ccmm-core test`
      size:       M
      issue:      #13
      pr:         #39

### Phase 2 — CLI

Goal: the shaded CLI jar analyzes, gates, and reports with `ccmm-core`. Exit criteria: `mvn -B -ntp -pl ccmm-cli -am verify` passes. This repository still has no `.ccmm.yml`, so `.github/workflows/quality-gate.yml` stays skipped.

#### P2-T01 Analyze command
- [x] P2-T01 Analyze command
      why:        Local inspection and the later gate command share one entry point.
      scope:      `ccmm-cli`
      depends on: P1-T08
      acceptance:
      - `analyze <path>` prints a text summary and `--format json` prints the report model.
      - A file that fails to parse is listed and the process still exits 0.
      - Bad arguments exit 2.
      verify:     `mvn -B -ntp -pl ccmm-cli -am verify`
      size:       M
      issue:      #14
      pr:         #40

#### P2-T02 Gate rule file
- [x] P2-T02 Gate rule file
      why:        The pull-request gate needs thresholds that are not hard-coded.
      scope:      `ccmm-cli`
      depends on: P1-T05
      acceptance:
      - `.ccmm.yml` parses `cyclomatic` and `nesting` as maximums and `maintainability` as a minimum.
      - A missing file and a malformed file are errors with a message.
      - Unit tests cover a valid file, a missing file, and a malformed file.
      verify:     `mvn -B -ntp -pl ccmm-cli -am test`
      size:       M
      issue:      #15
      pr:         #41

#### P2-T03 Gate command
- [x] P2-T03 Gate command
      why:        Reviewers need a pass or fail on the methods a branch actually changed.
      scope:      `ccmm-cli`
      depends on: P2-T01, P2-T02
      acceptance:
      - `gate --base <ref>` limits analysis to Java files changed versus that git ref.
      - Exit 1 when a changed method breaks a rule. Exit 0 when none do.
      - `--format markdown` writes the summary the quality-gate workflow posts.
      - This task does not add `.ccmm.yml` to this repository.
      verify:     `mvn -B -ntp -pl ccmm-cli -am verify`
      size:       L
      issue:      #16
      pr:         #42

#### P2-T04 Report command
- [x] P2-T04 Report command
      why:        A saved report is the offline view of the same numbers the IDE shows.
      scope:      `ccmm-cli`
      depends on: P2-T01
      acceptance:
      - `report <path> --format markdown` writes one section per file and a table of methods.
      - Failed parses are listed and are not given invented metrics.
      verify:     `mvn -B -ntp -pl ccmm-cli -am verify`
      size:       M
      issue:      #17
      pr:         #43

### Phase 3 — IDE

Goal: open a folder, edit a Java file, and see live metrics. Exit criteria: the three IDE benchmarks exist and `mvn -B -ntp -pl ccmm-app -am test` passes. If P0-T01 is `no-go`, P3-T02 stops for a decision instead of shipping RichTextFX.

#### P3-T01 Window and file tree
- [x] P3-T01 Window and file tree
      why:        Metrics are useless until a project is visible.
      scope:      `ccmm-app` and `docs/benchmarks/cold-start.md`
      depends on: P0-T01
      acceptance:
      - The window opens a directory and lists its files.
      - Directory walking is not on the JavaFX thread.
      - `docs/benchmarks/cold-start.md` records time to a usable window against the 2 s budget.
      verify:     `mvn -B -ntp -pl ccmm-app -am test`
      size:       L
      issue:      #18
      pr:         #44

#### P3-T02 Editor
- [x] P3-T02 Editor
      why:        The editing surface has to meet the keystroke budget before metrics are attached.
      scope:      `ccmm-app` and `docs/benchmarks/keystroke-latency.md`
      depends on: P3-T01
      acceptance:
      - Opening a `.java` file shows it in a RichTextFX `CodeArea`.
      - Typing does not run the analyzer.
      - If P0-T01 was `no-go`, stop with `Decision needed:` and do not add the editor.
      - Otherwise append a fresh p95 measurement to `docs/benchmarks/keystroke-latency.md`.
      verify:     `mvn -B -ntp -pl ccmm-app -am test`
      size:       L
      issue:      #19
      pr:         #45

#### P3-T03 Debounced analysis
- [x] P3-T03 Debounced analysis
      why:        Parsing on the JavaFX thread would miss the keystroke budget.
      scope:      `ccmm-app` and `docs/benchmarks/metrics-refresh.md`
      depends on: P3-T02, P1-T07
      acceptance:
      - Edits wait about 300 ms, then analyze off the JavaFX thread.
      - A newer edit cancels or drops the older result.
      - The panel update is applied with `Platform.runLater`.
      - `docs/benchmarks/metrics-refresh.md` records the refresh time against the 300 ms budget.
      verify:     `mvn -B -ntp -pl ccmm-app -am test`
      size:       L
      issue:      #20
      pr:         #46

#### P3-T04 Metrics panel
- [x] P3-T04 Metrics panel
      why:        The legacy sidebar's job was to show MI, complexity, LOC, and loop depth without failing the editor.
      scope:      `ccmm-app`
      depends on: P3-T03
      acceptance:
      - The panel shows maintainability index, cyclomatic complexity, code lines, Halstead volume, and loop nesting for the file and for the method at the caret.
      - A parse failure keeps the last good metrics and marks them stale.
      - A buffer that has never parsed shows the message and no metrics. No exception reaches the UI.
      verify:     `mvn -B -ntp -pl ccmm-app -am test`
      size:       M
      issue:      #21
      pr:         #47

### Phase 4 — Collaboration

Goal: co-edit one file, with chat and presence on the project room. ADR-0002, ADR-0005, and ADR-0006 are accepted. Exit criteria: two IDE processes converge on concurrent inserts, a wrong invite token is rejected, and the file tree is not sent over the socket.

#### P4-T01 Protocol model
- [x] P4-T01 Protocol model
      why:        The IDE and the relay have to share one message definition.
      scope:      `ccmm-collab-protocol`
      depends on: P0-T02, #3
      acceptance:
      - Messages cover join, invite token, text insert, text delete, presence, and chat.
      - A text op carries the revision it was based on.
      - Tests extend the P0-T02 convergence cases. No WebSocket server in this task.
      verify:     `mvn -B -ntp -pl ccmm-collab-protocol test`
      size:       L
      issue:      #22
      pr:         #49

#### P4-T02 Path checks
- [x] P4-T02 Path checks
      why:        A peer-supplied path must not read or write outside the project.
      scope:      `ccmm-collab-protocol` or `ccmm-core`
      depends on: P4-T01, #3
      acceptance:
      - Absolute paths, paths containing `..`, and symlinks that escape the project root are rejected.
      - Tests cover each rejection and one accepted relative path.
      verify:     `mvn -B -ntp -pl ccmm-collab-protocol -am test`
      size:       S
      issue:      #23
      pr:         #50

#### P4-T03 Relay server
- [x] P4-T03 Relay server
      why:        Co-editing needs the process ADR-0005 already publishes.
      scope:      `ccmm-collab-server`
      depends on: P4-T01, P4-T02, #3
      acceptance:
      - A WebSocket relay hosts a project room (presence, chat) and per-file rooms (text ops).
      - A missing or wrong invite token is disconnected.
      - Concurrent ops follow the P0-T02 transform.
      - `docker build` of `ccmm-collab-server/Dockerfile` still succeeds after `mvn -pl ccmm-collab-server -am package`.
      verify:     `mvn -B -ntp -pl ccmm-collab-server -am verify`
      size:       L
      issue:      #24
      pr:         #51

#### P4-T04 Co-editing client
- [x] P4-T04 Co-editing client
      why:        Remote edits have to land on the JavaFX thread without breaking local undo.
      scope:      `ccmm-app`
      depends on: P4-T03, P3-T02, #3
      acceptance:
      - Remote ops for one frame are applied in a single batched edit.
      - Undo reverts local typing only.
      - A path from the relay is rejected unless it passes P4-T02.
      verify:     `mvn -B -ntp -pl ccmm-app -am test`
      size:       L
      issue:      #25
      pr:         #52

#### P4-T05 Chat and presence
- [x] P4-T05 Chat and presence
      why:        v1 collaboration includes seeing who is here and talking, without sharing the tree.
      scope:      `ccmm-app`
      depends on: P4-T03, P3-T01, #3
      acceptance:
      - The project room shows connected display names and chat messages.
      - No message creates, renames, or deletes a file.
      verify:     `mvn -B -ntp -pl ccmm-app -am test`
      size:       M
      issue:      #26
      pr:         #53

### IDE-grade UX program

Version 3 adds this program on top of the completed phases above. Ids are `E<epic>-T<nn>` and do not reuse a `P*` id. Spikes gate the epics they inform. E4 does not wait on the editor bake-off. E6 expands the non-goal "running code" and may be deleted by the owner. E7 extends Phase 4; it does not replace the relay.

#### E0 — spikes

- [x] E0-T01 Compare editor components
      why:        Folding, a 5 MB file, and gutter decorations were never measured.
      scope:      spikes/e0-t01/ and docs/benchmarks/editor-bakeoff.md
      depends on: none
      acceptance: The benchmark file records both components and ends with go or no-go.
      verify:     The file exists and mvn -B -ntp -pl ccmm-app -am test still passes. Spike code is not on the app classpath.
      size:       L
      kind:       spike
      issue:      #54
      pr:         #126

- [x] E0-T02 Compare highlighting engines
      why:        Highlighting can stall typing, and the engine is undecided.
      scope:      spikes/e0-t02/ and docs/benchmarks/highlighting.md
      depends on: none
      acceptance: The benchmark names a go engine or a no-go plan change.
      verify:     docs/benchmarks/highlighting.md ends with go or no-go.
      size:       M
      kind:       spike
      issue:      #55
      pr:         #127

- [x] E0-T03 Prove anchors survive operational transform
      why:        Comment threads have to move with the text or they point at the wrong lines.
      scope:      ccmm-collab-protocol tests and docs/benchmarks/anchor-ot.md
      depends on: none (Phase 4 OT is done)
      acceptance: Two clients converge on text and on an anchor after inserts above it.
      verify:     mvn -B -ntp -pl ccmm-collab-protocol test
      size:       M
      kind:       spike
      issue:      #56
      pr:         #128

- [x] E0-T04 Measure theme tokens at high DPI
      why:        A token set that fails contrast or blurs at 150% is not a theme.
      scope:      spikes/e0-t04/ and docs/benchmarks/hidpi.md
      depends on: none
      acceptance: 100%, 150%, and 200% are recorded, with a go or a token fix.
      verify:     docs/benchmarks/hidpi.md
      size:       M
      kind:       spike
      issue:      #57
      pr:         #129

- [x] E0-T05 Prove headless UI tests on CI
      why:        IdeWindowTest skips when the JVM is headless, and Ubuntu CI has no display.
      scope:      spikes/e0-t05/ and docs/benchmarks/ui-tests.md
      depends on: none
      acceptance: The note says whether Xvfb or Monocle makes IdeWindowTest run on all three OS.
      verify:     docs/benchmarks/ui-tests.md. Do not edit .github/workflows until ADR-0015 is accepted.
      size:       M
      kind:       spike
      issue:      #58
      pr:         pending

#### E1 — workbench

- [ ] E1-T01 Add workbench regions
      why:        One BorderPane cannot host the inspector, the panel, and the status bar.
      scope:      ccmm-app workbench package
      depends on: none
      acceptance: The window shows the six regions and the editor keeps at least 65 percent of a 1280-wide window.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       L
      kind:       ui
      issue:      #59

- [ ] E1-T02 Add the activity bar
      why:        The side bar needs a way to switch views.
      scope:      ccmm-app workbench
      depends on: E1-T01
      acceptance: Five buttons switch the side bar and expose accessible names.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       M
      kind:       ui
      issue:      #60

- [ ] E1-T03 Add the side bar shell
      why:        Views need a title and a collapse control.
      scope:      ccmm-app workbench
      depends on: E1-T02
      acceptance: The side bar shows the active view title and can collapse.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       S
      kind:       ui
      issue:      #61

- [ ] E1-T04 Add the bottom panel shell
      why:        Problems, metrics, terminal, and chat need a shared panel.
      scope:      ccmm-app workbench
      depends on: E1-T01
      acceptance: Tabs switch and Ctrl+J collapses the panel.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       M
      kind:       ui
      issue:      #62

- [ ] E1-T05 Add the status bar shell
      why:        Position, encoding, and metrics need a home that is not the inspector.
      scope:      ccmm-app workbench
      depends on: E1-T01
      acceptance: The status bar shows line, column, and the open-folder error.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       M
      kind:       ui
      issue:      #63

- [ ] E1-T06 Add a command registry
      why:        The palette and the menu must run the same commands.
      scope:      ccmm-app workbench
      depends on: E1-T01
      acceptance: Open Directory is a command the registry can run.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       M
      kind:       code
      issue:      #64

- [ ] E1-T07 Add a keymap
      why:        Shortcuts must be one table, with Ctrl on Windows and Linux and Cmd on macOS.
      scope:      ccmm-app workbench
      depends on: E1-T06
      acceptance: The table maps Ctrl+B and Cmd+B to the same command id.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       M
      kind:       code
      issue:      #65

- [ ] E1-T08 Apply theme tokens
      why:        The window is still Modena.
      scope:      ccmm-app stylesheets
      depends on: E0-T04, ADR-0013 accepted, ADR-0014 accepted for the font
      acceptance: Dark is the default and light is selectable once settings exist. Tokens drive surfaces.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       M
      kind:       ui
      issue:      #66

- [ ] E1-T09 Add a settings store
      why:        Theme, font size, relay URL, and display name have to survive a restart.
      scope:      ccmm-app settings
      depends on: E1-T06
      acceptance: Values round-trip through the OS config directory.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       M
      kind:       code
      issue:      #67

- [ ] E1-T10 Restore the session
      why:        Reopening the IDE should bring back the folder and the open files.
      scope:      ccmm-app session
      depends on: E1-T09, E2-T01
      acceptance: The last folder and pinned paths are restored.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       M
      kind:       code
      issue:      #68

#### E2 — editor

- [ ] E2-T01 Add a document model
      why:        The editor state lives in IdeWindow fields, so tabs and remote edits have nowhere to sit.
      scope:      ccmm-app document
      depends on: none
      acceptance: A document tracks text, dirty, and local versus remote edits.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       L
      kind:       code
      issue:      #69

- [ ] E2-T02 Add a tab strip
      why:        One buffer cannot show the files a review touches.
      scope:      ccmm-app editor tabs
      depends on: E1-T01, E2-T01
      acceptance: Preview, pin, dirty, and close behave as the UI spec.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       L
      kind:       ui
      issue:      #70

- [ ] E2-T03 Highlight the open buffer
      why:        Plain text does not look like an editor, and non-Java files are unreadable today.
      scope:      ccmm-app highlight
      depends on: E0-T01, E0-T02, E2-T01, ADR-0009 decision
      acceptance: A Java buffer is styled off the FX thread and a keystroke stays under 16 ms p95.
      verify:     mvn -B -ntp -pl ccmm-app -am test and a row in docs/benchmarks/highlighting.md for the product path.
      size:       L
      kind:       ui
      issue:      #71

- [ ] E2-T04 Find and replace in the buffer
      why:        There is no way to find a word in the open file.
      scope:      ccmm-app editor find
      depends on: E2-T01, E1-T07
      acceptance: Ctrl+F and Ctrl+H find and replace without blocking the FX thread on a 5 MB file.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       M
      kind:       ui
      issue:      #72

- [ ] E2-T05 Go to a line
      why:        Metrics and problems cite line numbers.
      scope:      ccmm-app editor
      depends on: E2-T01, E1-T07
      acceptance: Ctrl+G moves the caret to a line.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       S
      kind:       ui
      issue:      #73

- [ ] E2-T06 Match brackets
      why:        A complexity editor should show which brace belongs to the caret.
      scope:      ccmm-app editor
      depends on: E2-T01
      acceptance: The matching bracket is highlighted, off the analysis path.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       M
      kind:       ui
      issue:      #74

- [ ] E2-T07 Auto-indent the next line
      why:        Enter should keep the indent of Java code.
      scope:      ccmm-app editor
      depends on: E2-T01
      acceptance: Enter copies indent and adds one level after an opening brace.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       M
      kind:       code
      issue:      #75

- [ ] E2-T08 Show encoding and line endings
      why:        The status bar has slots and the file's ending is invisible.
      scope:      ccmm-app document
      depends on: E1-T05, E2-T01
      acceptance: The status bar shows LF or CRLF and UTF-8 or a detected label.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       M
      kind:       ui
      issue:      #76

- [ ] E2-T09 Add large-file mode
      why:        A 5 MB file must not run highlighting or JavaParser on every pause.
      scope:      ccmm-app document
      depends on: E2-T01
      acceptance: Files over 1 MiB open without highlighting or analysis, with a banner.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       M
      kind:       code
      issue:      #77

- [ ] E2-T10 Fold regions the bake-off allows
      why:        Long methods are the thing this IDE is about, and folding makes them readable.
      scope:      ccmm-app editor
      depends on: E0-T01
      acceptance: If the bake-off says folding is feasible, brace regions fold. If not, the issue closes with the benchmark citation and no widget code.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       L
      kind:       ui
      issue:      #78

#### E3 — navigation

- [ ] E3-T01 Open files by name
      why:        The heliogrid tree is 31,847 nodes. Typing a name must not walk it.
      scope:      ccmm-app navigation
      depends on: E1-T07, E4-T01, E2-T02
      acceptance: Ctrl+P lists matching project-relative paths and does not read ignored directories.
      verify:     mvn -B -ntp -pl ccmm-app -am test. Note the index time in docs/benchmarks/quick-open.md against a fixture with a 30,000-file ignored directory.
      size:       L
      kind:       ui
      issue:      #79

- [ ] E3-T02 Add the command palette
      why:        Every command has to be reachable without a menu hunt.
      scope:      ccmm-app navigation
      depends on: E1-T06, E1-T07
      acceptance: Ctrl+Shift+P runs a registered command.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       M
      kind:       ui
      issue:      #80

- [ ] E3-T03 Search the project
      why:        Find in one file is not enough to review a change.
      scope:      ccmm-app search
      depends on: E1-T02, E4-T01
      acceptance: Search skips ignored directories and caps the hit list.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       L
      kind:       ui
      issue:      #81

- [ ] E3-T04 Show the file outline
      why:        Methods are the unit of complexity and the tree does not list them.
      scope:      ccmm-app outline
      depends on: E1-T02, E2-T01
      acceptance: A Java file lists methods and a click moves the caret.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       M
      kind:       ui
      issue:      #82

- [ ] E3-T05 Show breadcrumbs
      why:        The path and the method at the caret should sit above the editor.
      scope:      ccmm-app editor
      depends on: E2-T02, E3-T04
      acceptance: Breadcrumbs show path segments and the method name.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       S
      kind:       ui
      issue:      #83

- [ ] E3-T06 Navigate back and forward
      why:        Jumping to a method needs a way back.
      scope:      ccmm-app navigation
      depends on: E2-T05
      acceptance: Alt+Left and Alt+Right walk the caret history.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       M
      kind:       code
      issue:      #84

- [ ] E3-T07 Remember recent files and projects
      why:        The welcome screen and quick open need a recent list.
      scope:      ccmm-app session
      depends on: E1-T09
      acceptance: The last 10 projects and 20 files round-trip.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       M
      kind:       code
      issue:      #85

- [ ] E3-T08 Add the welcome screen
      why:        An empty editor does not tell the user what to do.
      scope:      ccmm-app welcome
      depends on: E3-T07, E1-T06
      acceptance: No-folder startup shows Open Folder, Start a Session, and recents.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       M
      kind:       ui
      issue:      #86

#### E4 — project

- [ ] E4-T01 Apply ignore rules
      why:        heliogrid's eager walk built 31,847 nodes in 3,257 ms, mostly junk.
      scope:      ccmm-app files and a small helper
      depends on: none
      acceptance: Default ignores skip .git, node_modules, and the patterns in .gitignore.
      verify:     mvn -B -ntp -pl ccmm-app -am test. Add docs/benchmarks/file-tree.md only as a note of the fixture time if this issue also changes the walk; the lazy walk benchmark is E4-T02.
      size:       M
      kind:       code
      issue:      #87

- [ ] E4-T02 Load the tree lazily
      why:        Even an ignore-aware eager walk still builds the whole remaining tree before first paint.
      scope:      ccmm-app files
      depends on: E4-T01
      acceptance: Collapsed directories have no children loaded, and the FX thread does not list them.
      verify:     mvn -B -ntp -pl ccmm-app -am test. docs/benchmarks/file-tree.md records time to first root on a fixture.
      size:       L
      kind:       ui
      issue:      #88

- [ ] E4-T03 Show file icons
      why:        Names alone are hard to scan.
      scope:      ccmm-app explorer
      depends on: E4-T02, ADR-0014 accepted
      acceptance: Java files show the file-code glyph and directories show folder.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       M
      kind:       ui
      issue:      #89

- [ ] E4-T04 Decorate git status
      why:        Reviewers need to see which files changed.
      scope:      ccmm-app git
      depends on: E4-T02, E1-T05
      acceptance: Modified files use the warn token and the status bar shows the branch.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       M
      kind:       ui
      issue:      #90

- [ ] E4-T05 Create, rename, delete, and move files
      why:        A project view that cannot rename a file is not finished.
      scope:      ccmm-app files
      depends on: E4-T02, E2-T01
      acceptance: The four operations update the tree and refuse paths that leave the root.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       L
      kind:       ui
      issue:      #91

- [ ] E4-T06 Reveal a file in the operating system
      why:        Users still need the system file manager.
      scope:      ccmm-app files
      depends on: E4-T02
      acceptance: The command opens the system folder for the selected path.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       S
      kind:       code
      issue:      #92

- [ ] E4-T07 Refresh when files change
      why:        A file saved by another tool never appears.
      scope:      ccmm-app files
      depends on: E4-T02
      acceptance: A created file appears, and macOS falls back to polling if the watch service fails.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       M
      kind:       code
      issue:      #93

- [ ] E4-T08 Add explorer context menus
      why:        The file operations need a mouse path.
      scope:      ccmm-app explorer
      depends on: E4-T05, E4-T06
      acceptance: Right-click shows the file commands.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       M
      kind:       ui
      issue:      #94

#### E5 — metrics

- [ ] E5-T01 Show gutter complexity chips
      why:        The differentiator is invisible unless the caret is in the side panel.
      scope:      ccmm-app metrics decorations
      depends on: E2-T01, E0-T01
      acceptance: Each method signature shows a CC chip that opens the inspector.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       L
      kind:       ui
      issue:      #95

- [ ] E5-T02 Heat the outline
      why:        The outline should sort the hot methods, not only list names.
      scope:      ccmm-app outline
      depends on: E3-T04, E5-T01
      acceptance: Rows show a heat bar and can sort by complexity.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       M
      kind:       ui
      issue:      #96

- [ ] E5-T03 Add the inspector
      why:        The label stack does not show decision points or rule status.
      scope:      ccmm-app inspector
      depends on: E1-T01, E5-T01, E2-T05
      acceptance: The inspector shows the caret method and jumps to a decision point.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       L
      kind:       ui
      issue:      #97

- [ ] E5-T04 Show metrics in the status bar
      why:        The eye already checks the status bar for the caret.
      scope:      ccmm-app status
      depends on: E1-T05, E5-T01
      acceptance: The bar shows CC and MI for the caret method, and the word stale.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       S
      kind:       ui
      issue:      #98

- [ ] E5-T05 List gate violations as problems
      why:        The CLI gate and the IDE must agree.
      scope:      ccmm-app problems, reuse ccmm-cli rules
      depends on: E1-T04, E2-T05
      acceptance: A method over the cyclomatic limit appears as a problem that jumps to the line.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       L
      kind:       ui
      issue:      #99

- [ ] E5-T06 Add the project dashboard
      why:        File-level chips do not show the worst methods in the repo.
      scope:      ccmm-app dashboard
      depends on: E2-T02, E5-T01
      acceptance: An editor tab lists the worst 10 methods.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       L
      kind:       ui
      issue:      #100

- [ ] E5-T07 Run a pre-flight gate
      why:        The CLI gate should be visible before a commit, not only in CI.
      scope:      ccmm-app gate
      depends on: E5-T05, E4-T04
      acceptance: Changed methods that break a rule are listed before any git write.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       M
      kind:       ui
      issue:      #101

- [ ] E5-T08 Keep stale metrics on the new surfaces
      why:        Stale behaviour exists on the label panel and must move with the chips.
      scope:      ccmm-app metrics
      depends on: E5-T01, E5-T03, E5-T04
      acceptance: A parse error keeps the last good numbers on chips, inspector, outline, and status bar.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       M
      kind:       ui
      issue:      #102

#### E6 — run

- [ ] E6-T01 Define build and run tasks
      why:        The owner may want build output inside the IDE. This epic expands the non-goal running code and can be deleted.
      scope:      ccmm-app tasks
      depends on: E1-T06
      acceptance: A task file can list a command that the IDE will not run until E6-T02.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       M
      kind:       code
      issue:      #103

- [ ] E6-T02 Add the output panel
      why:        Task output needs a tab that is not the editor.
      scope:      ccmm-app output
      depends on: E1-T04, E6-T01
      acceptance: Running a task appends its output off the FX thread.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       M
      kind:       ui
      issue:      #104

- [ ] E6-T03 Embed a terminal
      why:        A local shell is the remaining run surface, and it must not be shared.
      scope:      ccmm-app terminal
      depends on: E6-T02
      acceptance: A local terminal tab runs in the project root and is not sent to the relay.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       L
      kind:       ui
      issue:      #105

#### E7 — collaboration

- [ ] E7-T01 Start and join a session
      why:        The app can render presence only after someone else injects messages. There is no connect UI.
      scope:      ccmm-app session client
      depends on: E1-T02, E1-T09, ADR-0011 accepted
      acceptance: The user can start or join with the existing invite token and see connection errors.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       L
      kind:       ui
      issue:      #106

- [ ] E7-T02 Compare file hashes on join
      why:        Two checkouts can diverge before anyone connects.
      scope:      ccmm-collab-protocol and ccmm-app
      depends on: E7-T01, ADR-0011 accepted
      acceptance: A mismatch shows a diff and Use host version replaces the local buffer.
      verify:     mvn -B -ntp -pl ccmm-collab-protocol,ccmm-app -am test
      size:       M
      kind:       code
      issue:      #107

- [ ] E7-T03 Draw remote carets
      why:        Co-editing without a visible peer caret is easy to overwrite.
      scope:      ccmm-app editor decorations
      depends on: E2-T01, E0-T01, E7-T01
      acceptance: A remote selection renders with the peer colour and does not scroll the viewport.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       L
      kind:       ui
      issue:      #108

- [ ] E7-T04 Follow a collaborator
      why:        Review walkthroughs need a voluntary way to track the host.
      scope:      ccmm-app follow
      depends on: E7-T03, E3-T06
      acceptance: Follow tracks file and caret, and any local navigation stops it.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       L
      kind:       ui
      issue:      #109

- [ ] E7-T05 Move chat into the bottom panel
      why:        Chat is a label dump that shares the inspector.
      scope:      ccmm-app chat
      depends on: E1-T04, E7-T01
      acceptance: Chat messages are a transcript with an entry field, and they do not change files.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       M
      kind:       ui
      issue:      #110

- [ ] E7-T06 Show presence on the tree and tabs
      why:        People need to see who is in a file without opening it.
      scope:      ccmm-app presence
      depends on: E2-T02, E4-T02, E7-T03
      acceptance: A coloured dot and the name sit on the file that peer has open.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       M
      kind:       ui
      issue:      #111

- [ ] E7-T07 Anchor comment threads
      why:        Review comments have to survive edits above them.
      scope:      ccmm-collab-protocol and ccmm-app
      depends on: E0-T03, E7-T01
      acceptance: A thread stays on the same characters after an insert above it.
      verify:     mvn -B -ntp -pl ccmm-collab-protocol,ccmm-app -am test
      size:       L
      kind:       ui
      issue:      #112

- [ ] E7-T08 Show the session metrics lens
      why:        Shared typing is not the product. Shared code health is.
      scope:      ccmm-app session
      depends on: E5-T08, E7-T01
      acceptance: The session view shows the cyclomatic delta since join.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       M
      kind:       ui
      issue:      #113

- [ ] E7-T09 Export a session summary
      why:        The review needs an artifact that can be pasted onto a pull request.
      scope:      ccmm-app session
      depends on: E7-T07, E7-T08
      acceptance: Export produces Markdown with files, deltas, and threads.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       M
      kind:       ui
      issue:      #114

- [ ] E7-T10 Enforce roles and approval
      why:        A viewer must not be able to write by any path.
      scope:      ccmm-collab-server and ccmm-app
      depends on: E7-T01
      acceptance: The server drops text ops from a viewer, and join waits for host approval.
      verify:     mvn -B -ntp -pl ccmm-collab-server,ccmm-app -am test
      size:       L
      kind:       code
      issue:      #115

- [ ] E7-T11 Hide ignored and secret files from guests
      why:        Guests should not receive .env files or gitignored paths.
      scope:      ccmm-app and ccmm-collab-server
      depends on: E4-T01, E7-T10
      acceptance: A guest cannot open a gitignored path or a .env file through the session.
      verify:     mvn -B -ntp -pl ccmm-collab-server,ccmm-app -am test
      size:       M
      kind:       code
      issue:      #116

- [ ] E7-T12 Reconnect without dropping local edits
      why:        A dropped socket must not throw away typing.
      scope:      ccmm-app session
      depends on: E7-T01, E2-T01
      acceptance: Local edits survive a disconnect and the status shows the buffered count.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       M
      kind:       code
      issue:      #117

#### E8 — polish

- [ ] E8-T01 Audit keyboard access and contrast
      why:        A keyboard-first IDE that traps focus is unfinished.
      scope:      ccmm-app
      depends on: E1-T08, E3-T02
      acceptance: Every activity button and the palette are reachable from the keyboard, and a contrast note is in the PR.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       M
      kind:       ui
      issue:      #118

- [ ] E8-T02 Check high DPI and the latency budgets
      why:        The spike was one machine. The product path needs the same numbers.
      scope:      docs/benchmarks and ccmm-app
      depends on: E1-T08, E2-T03, E4-T02
      acceptance: Keystroke p95, cold start, and first tree paint are re-recorded on the product window.
      verify:     The three benchmark files gain a product-path section that ends go or names the regression.
      size:       M
      kind:       ui
      issue:      #119

- [ ] E8-T03 Audit empty and error copy
      why:        The UI spec names the sentences. The screens need to match.
      scope:      ccmm-app
      depends on: E3-T08, E5-T05, E7-T01
      acceptance: A test lists the empty and error strings from the UI spec and finds them in the UI.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       S
      kind:       ui
      issue:      #120

- [ ] E8-T04 Add onboarding hints
      why:        The welcome screen lists shortcuts once. The first session should point at metrics.
      scope:      ccmm-app welcome
      depends on: E3-T08, E5-T01
      acceptance: The first Java file shows one dismissible hint.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       S
      kind:       ui
      issue:      #121

#### E9 — release

- [ ] E9-T01 Document signing and notarization
      why:        Installers exist and signing is still optional tribal knowledge.
      scope:      docs/RELEASING.md
      depends on: none
      acceptance: The releasing doc names the secrets, the OS each one serves, and what stays unsigned when they are absent.
      verify:     The doc links from PLAN.md E9-T01. No workflow edit.
      size:       S
      kind:       docs
      issue:      #122

- [ ] E9-T02 Check for an update
      why:        Installed builds have no way to notice a newer release.
      scope:      ccmm-app update
      depends on: E1-T05
      acceptance: The status bar can show that a newer version exists, without downloading it.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       M
      kind:       code
      issue:      #123

- [ ] E9-T03 Record the crash and log location
      why:        A failed session has nowhere documented to look.
      scope:      ccmm-app logging and docs/RELEASING.md
      depends on: E1-T09
      acceptance: Logs go to the OS config directory and the doc names the path.
      verify:     mvn -B -ntp -pl ccmm-app -am test
      size:       S
      kind:       code
      issue:      #124

## Change log

| Version | Change | Reason | PR |
|---|---|---|---|
| 1 | Initial proposed plan | Planning protocol, issue #1 | #27 |
| 2 | Accept ADR-0002, ADR-0004, ADR-0005, ADR-0006, and ADR-0007 | Owner decision, issue #3 | #48 |
| 3 | Propose the IDE-grade UX program on top of the finished phases | Verified assessment, design system, proposed ADR-0008 through ADR-0015, issues #54-#124. Version 2 stays the approved baseline. | #125 |
