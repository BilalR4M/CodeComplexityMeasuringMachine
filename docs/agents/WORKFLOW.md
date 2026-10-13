# Workflow

One task per run. Every task passes these states in order. Do not skip or reorder.

| # | State | Do | Exit when |
|---|---|---|---|
| 0 | PLAN | `docs/PLAN.md` must say `Status: APPROVED`. If it does not, run `/plan` (PLANNING.md) and stop. | plan approved by owner |
| 1 | PICK | Take the first open `agent-task` issue (lowest phase, then task ID) whose dependencies are closed. `gh issue edit <n> --add-assignee @me`. | one task chosen |
| 2 | BRANCH | `git switch feat/ccmm-ide && git pull --ff-only && git switch -c <type>/<task-id>-<slug>` | clean branch from latest base |
| 3 | IMPLEMENT | Smallest change that meets the task's acceptance criteria. Tests first where practical. | acceptance tests pass locally |
| 4 | VERIFY | `mvn -B -ntp verify`; run the task's benchmark if it has one; self-review per REVIEW.md. | all green, self-review clean |
| 5 | COMMIT | Atomic Conventional Commits per COMMITS.md. | history is clean and buildable per commit |
| 6 | PR | Draft PR per PULL_REQUESTS.md, mark ready when CI is green on all 3 OS. | PR ready |
| 7 | REVIEW LOOP | Answer every comment; push follow-up commits. | owner approves |
| 8 | MERGE | Owner squash-merges. Agents never merge. | merged |
| 9 | CLOSE-OUT | Confirm the issue closed; file follow-ups as new `agent-task` issues. | nothing left dangling |

## Decision needed (stop protocol)
If blocked by an ambiguity, a pending ADR, or an "ask first" item:
1. Comment on the issue or PR with the heading `Decision needed:`, 2–3 options, trade-offs, and your recommendation.
2. Add the `question` label. Stop. Do not guess and continue.

## Definition of done
- Acceptance criteria in the issue are all demonstrably true (state how in the PR).
- `mvn -B -ntp verify` green on Linux, Windows, macOS CI.
- Tests cover new behaviour; benchmarks recorded if perf-relevant.
- docs/PLAN.md task ticked with the PR linked; ADR/docs updated if a decision changed.
- No TODO/FIXME without an issue number.
