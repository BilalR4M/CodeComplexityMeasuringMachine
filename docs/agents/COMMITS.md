# Commits and branches

## Branch names
`<type>/<task-id>-<slug>` — e.g. `feat/p1-t03-method-metrics`, `fix/p3-t07-stale-marker`.
Types: `feat fix perf refactor test docs build ci chore`. Base is `feat/ccmm-ide` until v1, then `main`. Branches are short-lived (one task). Rebase on the base before opening a PR; never merge the base into your branch.

## Commit message
```
<type>(<scope>): <subject>

<body: why, not what; wrap at 72>

Refs: #<issue>
```
- **type**: `feat` (user-visible capability) · `fix` · `perf` · `refactor` · `test` · `docs` · `build` · `ci` · `chore` · `revert`
- **scope** (optional but preferred): `core cli app collab protocol server ci build docs release deps`
- **subject**: imperative, lower-case start, no trailing period, <= 72 chars.
- **body**: explain motivation and trade-offs; mention benchmark numbers for `perf`.
- **footer**: `Refs: #n`. Use `Closes #n` only in the PR body.
- **breaking change**: `!` after type/scope plus a `BREAKING CHANGE:` footer. Ask the owner first (it forces a major version bump).

Release impact (release-please): `feat` → minor · `fix`, `perf` → patch · others appear in no bump. Choose the type for the release note a user should read.

## Atomic commits
- One logical change per commit; each commit builds and passes tests.
- Tests live in the same commit as the code they cover.
- No WIP, "fix typo", or "address review" commits in the final PR: use `git commit --fixup` + `git rebase -i --autosquash` before marking ready, or rely on squash merge (the PR title is then what matters).
- Do not mix formatting-only changes with logic.

## Examples
| Good | Bad |
|---|---|
| `feat(core): compute per-method cyclomatic complexity` | `update stuff` |
| `fix(app): keep last good metrics when parse fails` | `fix: bug` |
| `perf(app): batch remote ops into one replaceText call` | `perf(app): Made it faster.` |
| `test(core): add golden parity fixtures for halstead` | `WIP` |

## Rules
- Commit timestamps are real. Never set `GIT_AUTHOR_DATE`/`GIT_COMMITTER_DATE` or `--date`, and never rewrite history to change dates.
- The `.githooks/commit-msg` hook enforces the format (`git config core.hooksPath .githooks`). Never bypass it with `--no-verify`.
- Never force-push a shared branch. Force-pushing your own unmerged task branch after a rebase is fine (`--force-with-lease`).
- Never commit secrets, installers, `target/`, or generated files.
