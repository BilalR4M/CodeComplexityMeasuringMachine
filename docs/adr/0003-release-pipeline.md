# ADR-0003: Release pipeline
Status: accepted
Decision: release-please (Conventional Commits) drives versions, changelog, tags and GitHub Releases. Maven libraries go to GitHub Packages, the relay image to ghcr.io, installers and CLI jar to the Release with checksums and build-provenance attestations. Signing is opt-in via secrets.
Consequences: versions are never edited by hand; baseline 1.0.0 because of macOS jpackage rules.
