# AGENTS.md — CCMM IDE

Entry point for every coding agent. Read fully, then follow the doc for your current step.

## Mission
Desktop IDE written in Java with live code-complexity metrics. Successor to the legacy web app (git tag `legacy-web-v1`). Windows, Linux, macOS.

## Locked decisions
- Java 21, JavaFX + RichTextFX, Maven multi-module. Java-only language support (JavaParser). No LSP in v1.
- Metrics are an in-process library (`ccmm-core`); IDE and CLI share one engine. No HTTP hop.
- Users: developer productivity (live metrics) and code review / quality gates (CLI, PR comments, reports).
- v1 includes collaboration: co-editing + chat + presence. Per-file rooms keyed by project-relative path; file tree not synced.
- Scale target: hundreds of files per project.

## Pending decisions (docs/adr) — proposed defaults, NOT accepted
- ADR-0002 server-authoritative OT for co-editing. No collaboration implementation until accepted.
- ADR-0004 metric formulas (JavaParser). Legacy regex results are not the oracle.
- ADR-0005 relay hosting: operators run the ghcr.io image.
- ADR-0006 room auth: invite token.
- ADR-0007 cognitive complexity is post-v1.

## Read order
| When | Read |
|---|---|
| Always | this file, docs/agents/WORKFLOW.md |
| No plan yet, or re-planning | docs/agents/PLANNING.md |
| Creating branches or commits | docs/agents/COMMITS.md |
| Opening or updating a PR | docs/agents/PULL_REQUESTS.md |
| Before marking a PR ready; reviewing | docs/agents/REVIEW.md |

Slash commands (Claude Code): `/plan` `/next` `/commit` `/pr` `/review` `/verify`.

## Layout
```
ccmm-core/             metrics engine, gate rules, report model (no UI)
ccmm-cli/              headless CLI: analyze | gate | report   (shaded *-all.jar)
ccmm-collab-protocol/  shared message + text-op model
ccmm-collab-server/    Java WebSocket relay (rooms, presence, chat), Docker image
ccmm-app/              JavaFX IDE
fixtures/legacy/       sample inputs from the legacy app (parity tests)
docs/PLAN.md           the plan (written by the agent via /plan, approved by the owner)
docs/agents/           agent process docs
docs/adr/              architecture decision records
scripts/package.sh     jpackage wrapper used by CI
.githooks/             commit-msg hook (Conventional Commits)
```

## Commands
```
git config core.hooksPath .githooks            # once per clone: enables commit-msg hook
mvn -B -ntp verify                             # build + test all modules
mvn -pl ccmm-app -am javafx:run                # run the IDE
mvn -pl ccmm-cli -am package                   # CLI -> ccmm-cli/target/ccmm-cli-*-all.jar
git worktree add ../ccmm-legacy legacy-web-v1  # read-only reference copy of the legacy app
```

## Engineering rules
- FX thread does UI only. Parsing, metrics, highlighting, file IO, networking run off-thread. Analysis is debounced (~300 ms) with latest-wins cancellation.
- Parse errors mid-edit keep the last good metrics and mark them stale; never throw to the UI.
- Remote collaboration edits go through one batched path on the FX thread; undo is local-only.
- Validate every project-relative path received from a peer (no absolute paths, no `..`, no symlink escape).
- Metrics changes need golden tests against `fixtures/`. Deviations from legacy output are documented in an ADR.
- Every behaviour change ships with a test. Perf-sensitive code ships with a benchmark or latency-harness result in docs/benchmarks/.

## Hard boundaries
- Never push to `main` or `feat/ccmm-ide`; never force-push a shared branch; never use `--no-verify`.
- Never edit `<revision>` in pom.xml or CHANGELOG.md (release-please owns them).
- Never commit secrets, certificates, or generated installers.
- Never alter commit timestamps; commits carry real dates (default git behaviour).
- Ask first (see "Decision needed" in WORKFLOW.md): new dependencies, public API/protocol changes, CI/release workflow changes, breaking changes.

## Release flow (automated; do not hand-roll)
`feat/ccmm-ide` is the integration branch until v1, then it merges to `main`. On `main`, release-please opens a release PR; merging it tags `vX.Y.Z`, creates the GitHub Release, and CI attaches installers, the CLI jar and checksums, publishes `ccmm-core` and `ccmm-collab-protocol` to GitHub Packages, and pushes `ghcr.io/<owner>/ccmm-collab-server`.
