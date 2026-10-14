# Collaboration concept: purposeful, not just shared typing

Positioning: JetBrains is sunsetting Code With Me and Live Share is in maintenance mode, so a Java-native IDE with built-in, metrics-aware collaboration has room to matter. Typing together is table stakes; the value is **shared understanding of code health**.

The relay, per-file OT, path checks, and the presence/chat text dump already exist (Phase 4). This document is the product behaviour on top of that. The file tree stays unsynced (ADR-0002). Ownership model C is proposed in ADR-0011.

## Session modes (a session has a purpose)

| Mode | Who | What is different |
|---|---|---|
| **Pair** | host + editors | Co-editing, remote carets, follow, chat |
| **Review walkthrough** | host drives; guests viewers/commenters | Read-only by default, anchored comment threads, metrics heat visible to all, gate preview |
| **Teach** | instructor + students | Raise-hand "ask to edit", host can grant/revoke per person, session summary for grading notes |

## Roles and permissions

Host (owner, approves joiners, can end session) · Editor · Viewer (read-only, can comment). Host can change roles live. Joining requires an invite token; host approval is on by default.

## Features by priority

**P0 (v1 core)**

- Start/join via invite link; presence list with avatar, colour, current file.
- Remote carets and selections with name flags; per-file co-editing (OT per ADR-0002).
- Follow mode: click an avatar to follow caret, scroll and file switches; **any manual navigation by you stops following** (show a clear "following X — stop" chip); "follow me" request from host.
- Chat (session-scoped).
- Join handshake: compare content hash per opened file; on mismatch show a diff and offer "adopt host's version" — avoids silent divergence between checkouts.
- Local-only undo; remote edits never move your viewport unless you follow.

**P1 (the meaningful part)**

- **Presence in the tree and tabs:** coloured dots show who is in which file; badge "2 editing this method" in the gutter.
- **Anchored threads:** comments attached to a range or method that survive edits (anchors use the same op stream); open/resolved states; thread list in the Collaboration view.
- **Shared metrics lens:** session bar shows aggregate delta ("+3 CC across 2 methods since start") and per-method contributors; gate status visible to everyone.
- **Session summary export (Markdown):** files touched, metric deltas, resolved/open threads, decisions; designed to paste into a PR description.
- **Ask to edit / hand-raise** for viewers.

**P2 / non-goals for v1**

- Audio/video, screen share, shared debugger. Read-only shared terminal output is P2.

## Ownership model (ADR to write; recommendation first)

- **A. Same checkout (current plan):** each client saves to its own disk. Cheap; risk of divergence → mitigated by the join handshake.
- **B. Host-owned workspace (Live Share/Zed style):** guests edit the host's buffers; file tree synced. Strongest consistency; more protocol.
- **C. Hybrid (recommended for v1):** A + handshake + explicit "sync from host" action. Revisit B for v2.

Also decide: what happens to unsaved shared edits when a participant leaves (keep local copy, prompt to save).

## Security and trust

- Validate every peer-supplied path (no absolute, no `..`, no symlink escape); cap message and document sizes; rate-limit.
- TLS (wss); invite tokens are single-purpose and expire; host approval default on.
- Hide gitignored and configurable-secret files (`.env*`, key files) from guests by default.
- Say plainly in the UI that the relay server sees shared file content.

## UX rules

- Never steal focus; never auto-scroll unless following; at most one toast per join/leave burst.
- 8 colour-blind-safe remote colours with AA contrast on both themes; also differentiate by name flag, not only colour.
- Reconnect states are visible ("Reconnecting…", buffered edits count) and never lose local edits.

## Acceptance for the epic

Three clients, mixed OS, scripted 10-minute edit load without divergence; follow mode works across file switches; a viewer cannot write by any path; a reviewer can leave an anchored thread that stays attached through edits above it.
