---
description: Full verification before opening a PR
---
Run `mvn -B -ntp verify`. Check `git diff --stat` for unintended files, confirm docs/PLAN.md reflects the work,
and confirm commit and PR titles follow Conventional Commits. Report failures with file and line.
