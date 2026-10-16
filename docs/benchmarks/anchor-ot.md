# Anchor OT

E0-T03. Proof is `AnchorTransformTest` in `ccmm-collab-protocol`.
An anchor is a half-open `[start, end)` in code units. It is transformed with the same server-ordered `TextOp` log as the document.

## Cases

- Insert above the range shifts both ends (10..20 + insert 5 at 0 → 15..25).
- Insert inside grows the end.
- Delete that shortens the range keeps a non-empty span.
- Delete that covers the range removes the anchor (empty optional, not a negative span).
- Two clients replaying the server log finish on the same text and the same anchor.

## Command

```
mvn -B -ntp -pl ccmm-collab-protocol test
```

## Decision

go
