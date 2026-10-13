# Review

## Self-review (before marking ready)
Run `git diff <base>...HEAD` and read it as a stranger.
- [ ] Matches the issue's acceptance criteria exactly; nothing extra.
- [ ] Threading: no parsing, metrics, IO, or networking on the FX thread; UI updates via `Platform.runLater`.
- [ ] Cancellation/debounce correct (latest-wins); no leaked executors or listeners.
- [ ] Parse failures and malformed input handled without exceptions reaching the UI.
- [ ] Collaboration inputs validated (paths, message sizes, version numbers); undo is local-only.
- [ ] Tests: new behaviour, edge cases, a failing-then-passing case for bug fixes; golden fixtures updated deliberately.
- [ ] Perf budgets respected; numbers attached if the change is perf-sensitive.
- [ ] No new dependency without prior approval; no secrets; no generated files.
- [ ] Docs, ADRs, and PLAN.md updated.

## Reviewing someone else's PR
Prefix each comment with a severity:
- `blocker:` wrong behaviour, data loss, security, broken threading rule, missing tests for risky code.
- `major:` design or maintainability problem that should change before merge.
- `nit:` style or preference; never blocks.
Review order: acceptance fit → correctness → threading/perf → security → tests → naming/docs.
End with a verdict: `approve`, `request changes`, or `comment`, plus a one-paragraph summary.
