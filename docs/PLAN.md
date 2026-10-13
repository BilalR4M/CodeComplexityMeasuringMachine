# PLAN
Status: APPROVED
Version: 1

The owner sets `Status: APPROVED` and adds the `plan-approved` label. No implementation starts before that. Phase 4 is `BLOCKED: ADR-0002`.

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

## Open decisions

- [ADR-0002](adr/0002-collaboration-algorithm.md) operational transform, proposed.
- [ADR-0004](adr/0004-metric-formulas.md) JavaParser formulas, proposed.
- [ADR-0005](adr/0005-relay-hosting.md) operator-run image, proposed.
- [ADR-0006](adr/0006-room-auth.md) invite token, proposed.
- [ADR-0007](adr/0007-cognitive-complexity.md) post-v1, proposed.

ADR-0004 is the default the Phase 1 tasks implement. Accepting or replacing it is an owner edit of that file. It does not block Phase 1, because the tasks say which formula to code. ADR-0002, ADR-0005, and ADR-0006 block Phase 4.

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
- [ ] P1-T07 Golden fixtures
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

#### P1-T08 Project scan
- [ ] P1-T08 Project scan
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

### Phase 2 — CLI

Goal: the shaded CLI jar analyzes, gates, and reports with `ccmm-core`. Exit criteria: `mvn -B -ntp -pl ccmm-cli -am verify` passes. This repository still has no `.ccmm.yml`, so `.github/workflows/quality-gate.yml` stays skipped.

#### P2-T01 Analyze command
- [ ] P2-T01 Analyze command
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

#### P2-T02 Gate rule file
- [ ] P2-T02 Gate rule file
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

#### P2-T03 Gate command
- [ ] P2-T03 Gate command
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

#### P2-T04 Report command
- [ ] P2-T04 Report command
      why:        A saved report is the offline view of the same numbers the IDE shows.
      scope:      `ccmm-cli`
      depends on: P2-T01
      acceptance:
      - `report <path> --format markdown` writes one section per file and a table of methods.
      - Failed parses are listed and are not given invented metrics.
      verify:     `mvn -B -ntp -pl ccmm-cli -am verify`
      size:       M
      issue:      #17

### Phase 3 — IDE

Goal: open a folder, edit a Java file, and see live metrics. Exit criteria: the three IDE benchmarks exist and `mvn -B -ntp -pl ccmm-app -am test` passes. If P0-T01 is `no-go`, P3-T02 stops for a decision instead of shipping RichTextFX.

#### P3-T01 Window and file tree
- [ ] P3-T01 Window and file tree
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

#### P3-T02 Editor
- [ ] P3-T02 Editor
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

#### P3-T03 Debounced analysis
- [ ] P3-T03 Debounced analysis
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

#### P3-T04 Metrics panel
- [ ] P3-T04 Metrics panel
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

### Phase 4 — Collaboration

`BLOCKED: ADR-0002`

Goal: co-edit one file, with chat and presence on the project room, after the owner accepts ADR-0002, ADR-0005, and ADR-0006. Exit criteria: two IDE processes converge on concurrent inserts, a wrong invite token is rejected, and the file tree is not sent over the socket.

Do not pick these tasks while the #3 is open.

#### P4-T01 Protocol model
- [ ] P4-T01 Protocol model
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

#### P4-T02 Path checks
- [ ] P4-T02 Path checks
      why:        A peer-supplied path must not read or write outside the project.
      scope:      `ccmm-collab-protocol` or `ccmm-core`
      depends on: P4-T01, #3
      acceptance:
      - Absolute paths, paths containing `..`, and symlinks that escape the project root are rejected.
      - Tests cover each rejection and one accepted relative path.
      verify:     `mvn -B -ntp -pl ccmm-collab-protocol -am test`
      size:       S
      issue:      #23

#### P4-T03 Relay server
- [ ] P4-T03 Relay server
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

#### P4-T04 Co-editing client
- [ ] P4-T04 Co-editing client
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

#### P4-T05 Chat and presence
- [ ] P4-T05 Chat and presence
      why:        v1 collaboration includes seeing who is here and talking, without sharing the tree.
      scope:      `ccmm-app`
      depends on: P4-T03, P3-T01, #3
      acceptance:
      - The project room shows connected display names and chat messages.
      - No message creates, renames, or deletes a file.
      verify:     `mvn -B -ntp -pl ccmm-app -am test`
      size:       M
      issue:      #26

## Change log

| Version | Change | Reason | PR |
|---|---|---|---|
| 1 | Initial proposed plan | Planning protocol, issue #1 | #27 |
