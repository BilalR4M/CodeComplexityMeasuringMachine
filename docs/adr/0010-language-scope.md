# ADR-0010: Language scope
Status: proposed

## Context
AGENTS.md locks "Java-only language support (JavaParser)". The approved plan's non-goals include non-Java metrics and language servers. The baseline screenshot is a TypeScript project. `IdeWindow.openFile` returns immediately unless the name ends with `.java`, so the editor stays blank and shows no explanation. The metrics engine is JavaParser and should stay that way (ADR-0004, ADR-0007).

## Options
1. The editor opens any text file and highlights a fixed set. Metrics, gates, and the dashboard stay Java-only. No language server.
2. Only `.java` files open. Other files stay unreadable, with an explicit message. This is today's behaviour plus an empty state.
3. Multi-language metrics in this program. That is a new engine, not a UX change.

## Recommendation
Option 1. The owner is asked to change the locked sentence to: "Java-only metrics (JavaParser). The editor opens any text file and highlights a fixed set. No LSP in v1."

Non-goals that stay: non-Java metrics, cognitive complexity, a language server, and syncing the file tree.

A file that is not valid UTF-8, or that is larger than the large-file threshold in E2-T09, still opens. Highlighting and metrics turn off. The user sees the banner from the UI spec.

## Consequences
Until this ADR is accepted, implementation issues that open non-Java files stay blocked. E2-T01 (the document model) can store any text. E5 continues to call `Analyzer` only for `.java`. Option 3 is out of this program.
