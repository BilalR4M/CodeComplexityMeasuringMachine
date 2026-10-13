# Planning protocol

Planning is the agent's job; approval is the owner's. No implementation starts before the plan is approved.

## When to plan
- `docs/PLAN.md` says `Status: NOT PLANNED`, or
- a spike or review invalidates the plan (budget missed, decision changed), or the owner asks.
Re-planning edits the existing plan and appends to its change log; it never silently rewrites history.

## Inputs (read all before writing anything)
1. AGENTS.md (locked + pending decisions), docs/adr/*, docs/RELEASING.md.
2. The legacy app: `git worktree add ../ccmm-legacy legacy-web-v1`. Inventory features, metric formulas (Halstead, cyclomatic, MI, LOC), supported languages, and known bugs. Use fixtures/legacy.
3. Owner targets (starting points, challenge them with evidence):

| Metric | Proposed target |
|---|---|
| Cold start to usable window | < 2 s |
| Keystroke latency p95 (10k-line file) | < 16 ms |
| Live metrics refresh after typing stops | ~300 ms |
| Memory, few-hundred-file project | < 500 MB |
| Initial project scan | < 3 s |

## Steps
1. **Discover** → `docs/planning/discovery.md`: legacy feature inventory, parity gaps, formula differences, unknowns, risks.
2. **Decide** → for each open question (ADR-0002 OT vs CRDT, relay hosting, room auth, cognitive complexity, anything new): options, trade-offs, a recommendation, in the matching ADR as `Status: proposed`. Do not mark ADRs accepted.
3. **Design** → `docs/architecture.md`: module APIs (signatures-level), threading model, live-metrics data flow (edit → debounce → parse → metrics → UI), collaboration data flow, gate/CLI data flow, packaging per OS, failure modes.
4. **Slice** → `docs/PLAN.md` using the format below. Spikes first, each with explicit go/no-go criteria and what plan change a "no-go" triggers.
5. **Check** → every task has a testable acceptance criterion and a verification command; dependencies form a DAG; no task needs > ~400 changed lines.
6. **Publish** → create labels/issues, open PR `docs: add project plan` into `feat/ccmm-ide`, set `Status: PROPOSED`, stop. The owner sets `APPROVED` (label `plan-approved`).

## docs/PLAN.md format
```
# PLAN
Status: NOT PLANNED | PROPOSED | APPROVED
Version: <n>   (increment on every re-plan)

## Goals / Non-goals
## Budgets            (measurable; each names the benchmark that proves it)
## Risks              (risk · likelihood · impact · mitigation · owner task)
## Open decisions     (link ADRs)
## Phases
### Phase <n> — <name>
Goal: <one sentence>   Exit criteria: <testable list>
- [ ] <ID> <title>
      why:        <one line>
      scope:      <modules/paths>
      depends on: <IDs or none>
      acceptance: <testable bullets>
      verify:     <command(s)>
      size:       S | M | L        (S <= ~100 lines, M <= ~250, L <= ~400)
      issue:      #<n>
## Change log
| Version | Change | Reason | PR |
```
Rules: task IDs are `P<phase>-T<nn>` and never reused. Size, not calendar dates. Phase 0 is spikes; phases that depend on a pending ADR are marked `BLOCKED: ADR-xxxx`.

## Issues
```
gh label create agent-task --force; gh label create "phase:<n>" --force; gh label create "size:S" --force   # M, L likewise
gh issue create --title "<ID> <title>" --label "agent-task,phase:<n>,size:<s>" --body "<why, scope, depends on, acceptance, verify, link to PLAN.md anchor>"
```
One issue per task. Put the issue number back into PLAN.md.

## Quality bar
A reviewer who has not read this repo could pick any task and finish it from the issue text alone.
