# Pull requests

## Opening
- One task per PR, base `feat/ccmm-ide` (until v1). Open as **draft** early (`gh pr create --draft --base feat/ccmm-ide`), mark ready only after VERIFY passes and CI is green on Linux, Windows and macOS.
- **Title = a Conventional Commit** (`feat(core): …`). It becomes the squash commit and the release note; `pr-title` CI enforces it.
- Body follows `.github/PULL_REQUEST_TEMPLATE.md`: Summary · Task (plan ID, `Closes #n`) · Changes · Verification · Risk/rollback · Checklist.
- Verification is evidence, not claims: commands run, results, benchmark numbers vs budget, screenshots for UI.
- Labels: copy `phase:*` and `size:*` from the issue.

## Size and scope
- Target <= 400 changed lines excluding generated files. Larger → split into stacked PRs and say so in each description.
- No drive-by refactors. Unrelated problems become new `agent-task` issues.
- The PR ticks the task in docs/PLAN.md and links itself there.

## Review loop
- Answer every comment: fix it, or reply with a reasoned "won't fix". Do not resolve threads you did not open.
- Push follow-up commits during review (squash merge collapses them); re-request review when ready.
- If a comment changes scope or needs an ADR change, use the `Decision needed:` protocol in WORKFLOW.md.

## Merging
- Owner squash-merges. Agents never merge or enable auto-merge.
- The squash message is the PR title plus a body summary; branch auto-deletes.
- After merge: confirm the issue closed, file follow-ups, stop.

## Blocked or red CI
- Red on one OS only: reproduce the OS-specific cause (paths, line endings, file watching, fonts) and fix it; never skip that OS.
- Flaky test: open an issue labelled `flaky`, link it in the PR; do not retry-until-green silently.
