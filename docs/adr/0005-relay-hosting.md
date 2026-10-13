# ADR-0005: Relay hosting
Status: accepted

## Context
Co-editing, chat, and presence need a process that every participant can reach. The release workflow already builds `ghcr.io/<owner>/ccmm-collab-server` from `ccmm-collab-server/Dockerfile`.

## Options
- Operators run that image themselves (`docker run`, a compose file, or a host they control). The IDE stores a URL and an invite token.
- The project runs a public multi-tenant relay. That adds accounts, isolation between projects, and an uptime commitment.

## Recommendation
Ship the image and let operators run it. v1 does not add a hosted service. The README already shows the `docker run` command.

## Consequences
There is no default public server. A collaboration session does not start until someone publishes a URL. Phase 4 server work stays blocked with ADR-0002 and ADR-0006.
