---
description: Open or update a pull request for the current branch
---
Read docs/agents/PULL_REQUESTS.md and docs/agents/REVIEW.md. Run `mvn -B -ntp verify`, do the self-review, rebase on the base branch,
push, then `gh pr create --draft --base feat/ccmm-ide` (or update the existing PR) with a Conventional Commit title and the
template filled with real verification evidence. Mark ready only when CI is green on all three OS.
