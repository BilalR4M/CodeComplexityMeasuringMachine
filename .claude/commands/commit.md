---
description: Create clean Conventional Commits from the working tree
---
Read docs/agents/COMMITS.md. Split the changes into atomic commits (code with its tests), each building green.
Use `git add -p` as needed, write messages in the required format, never override dates, never use --no-verify.
Show `git log --oneline <base>..HEAD` at the end.
