# Headless UI tests

E0-T05. Subject: `IdeWindowTest` (6 methods). ADR-0015 stays proposed; `.github/workflows/ci.yml` is unchanged.

## Baseline (CI run 38935602285 on `feat/ccmm-ide`)

| OS | Command | IdeWindowTest |
|---|---|---|
| ubuntu-latest | `mvn -B -ntp verify` | skip (6 skipped) |
| windows-latest | `mvn -B -ntp verify` | pass (0 skipped) |
| macos-latest | `mvn -B -ntp verify` | pass (0 skipped) |

## Linux probe (spike run 38936761522 on `ubuntu-latest`)

Package: `xvfb` (present on the runner; otherwise `sudo apt-get install -y xvfb`).

| Harness | Command | IdeWindowTest |
|---|---|---|
| bare | `mvn -B -ntp -pl ccmm-app -am test -Dtest=IdeWindowTest -Dsurefire.failIfNoSpecifiedTests=false` | skip (6 skipped) |
| Xvfb | `xvfb-run -a mvn -B -ntp -pl ccmm-app -am test -Dtest=IdeWindowTest -Dsurefire.failIfNoSpecifiedTests=false` | pass (6 run, 0 skipped) |

Monocle was not tried. TestFX was not added.

## Chosen harness

**Xvfb** on Linux (`xvfb-run -a`). Windows and macOS keep a plain `mvn` invocation.

## Decision

go

Windows and macOS already execute. Linux executes under `xvfb-run`. Applying that to `ci.yml` waits on ADR-0015 acceptance (ask-first).
