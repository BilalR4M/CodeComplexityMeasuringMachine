# ADR-0006: Room authentication
Status: proposed (default pending owner decision)

## Context
The legacy Socket.IO server joins anyone who knows the room id and rejects only a duplicate username. The desktop relay will hold source text.

## Options
- Invite token. The person who creates a session gets a secret. Clients send it on connect. The server rejects a missing or wrong token. The token is the only credential in v1.
- User accounts. The server stores users and maps them to projects. That needs a user store, password or OAuth, and a recovery flow.

## Recommendation
Use an invite token on the project room. Per-file rooms accept only connections that already joined the project room with that token. Usernames stay display names and need not be unique.

## Consequences
A leaked token is a leaked project session. Rotating it is a new token, not an account ban. Phase 4 must not ship an open relay.
