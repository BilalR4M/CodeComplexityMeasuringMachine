# ADR-0002: Collaboration algorithm
Status: accepted

## Context
v1 needs co-editing, chat, and presence. A central relay always exists (ADR-0005). Rooms are per file, keyed by the project-relative path. The file tree is not synced. Participants are expected to be online together; offline edit and peer-to-peer sync are out of scope.

## Options
- Server-authoritative operational transform on plain-text inserts and deletes. The relay assigns a total order, transforms concurrent ops against that order, and broadcasts the result. Clients keep a local undo stack that does not include remote ops.
- A CRDT (for example a text RGA or a sequence CRDT). Peers can merge without a server, including after a disconnect. The document carries tombstones or other metadata, and the server becomes a store rather than the authority.

## Recommendation
Use server-authoritative OT. The relay is already required for presence and chat, the edits are plain text, and v1 does not need offline merge. A CRDT is the better fit if offline or peer-to-peer editing becomes a requirement, and it costs more metadata and a harder undo story.

## Consequences
Phase 4 in `docs/PLAN.md` is blocked until this ADR is accepted or replaced. Spike P0-T02 must show that two clients converge on concurrent inserts and deletes. A no-go replaces this proposal; it does not silently switch the implementation to a CRDT.
