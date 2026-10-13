# Releasing

1. Merge PRs with Conventional Commit titles into `main`.
2. release-please opens/updates a release PR (version bump + CHANGELOG). Merge it to cut `vX.Y.Z`.
3. The `release` workflow then: publishes Maven packages to GitHub Packages, builds installers on 4 runners
   (windows-x64, linux-x64, macos-arm64, macos-x64), attests provenance, attaches installers + SHA256SUMS + CLI jar to the
   Release, and pushes `ghcr.io/<owner>/ccmm-collab-server:<version>` and `:latest`.

## Repo settings (done by `bootstrap --push`, verify once)
- Settings → Actions → General → allow GitHub Actions to create and approve pull requests.
- Squash merge only; delete branch on merge; `main` protected with the CI checks.
- Optional secret `RELEASE_PLEASE_TOKEN` (PAT, `repo` scope): without it, release PRs created by GITHUB_TOKEN do not
  trigger CI, so required checks never run on them (admin override works because admins are not enforced).

## Optional signing secrets (pipeline is inert without them)
| Secret | Purpose |
|---|---|
| WINDOWS_CERT_PFX_BASE64, WINDOWS_CERT_PASSWORD | Authenticode signing of the msi |
| MACOS_CERT_P12_BASE64, MACOS_CERT_PASSWORD, MACOS_SIGN_IDENTITY | Developer ID signing |
| APPLE_ID, APPLE_TEAM_ID, APPLE_APP_PASSWORD | notarytool submission + stapling |

## Version note
macOS `jpackage` requires a major version >= 1, so the baseline is 1.0.0 (see `.release-please-manifest.json`).
