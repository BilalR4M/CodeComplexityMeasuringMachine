# ADR-0011: Collaboration ownership
Status: proposed

## Context
Phase 4 ships server-authoritative OT for one file, path checks, a relay, and a presence/chat text dump. Each client has its own checkout. The file tree is not synced (ADR-0002). Two people can still diverge silently when their copies of a file differ before they connect. `docs/design/COLLABORATION.md` asks for a handshake and an explicit sync, without turning the relay into a shared filesystem.

## Options
- A. Same checkout, as today. Cheap. Divergence is silent.
- B. Host-owned workspace. Guests edit the host's buffers and see the host's tree. Strong consistency. A larger protocol, and it conflicts with "the file tree is not synced".
- C. Hybrid. Each client still saves to its own disk. On join, the clients compare a content hash per opened file. A mismatch shows a diff and offers "Use host version". A later "Sync from host" repeats that for the open file. The tree is still local.

Unsaved edits when someone leaves: keep the local buffer and prompt `Save changes to <name>?` with `Save`, `Don't Save`, `Cancel`. Do not push the buffer to the relay as a file write.

## Recommendation
Option C. Revisit B only if a later plan accepts syncing the tree. Invite tokens, host approval, and wss stay as ADR-0005 and ADR-0006. The UI states that the relay sees shared file content.

E0-T03 proves that a comment anchor (path, start offset, end offset) transformed with the same OT as the text still points at the same characters after concurrent inserts above it. A no-go blocks E7-T07 and does not invent a second CRDT.

## Consequences
E7 extends the existing relay. It does not replace it. New message types (handshake, follow, anchor, role) are specified on the E7 issues. Adding them is a protocol change; the implementing agent stops with `Decision needed` if the owner has not accepted this ADR.
