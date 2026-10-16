# ADR-0015: UI test strategy
Status: proposed

## Context
`IdeWindowTest` starts a `Stage` and assumes `GraphicsEnvironment.isHeadless()` is false. CI (`ubuntu-latest`, `windows-latest`, `macos-latest`) runs `mvn verify` with no virtual framebuffer. On a headless runner those tests skip, so a green Linux job does not prove the window. TestFX and Monocle are the usual way to run JavaFX without a display. Neither is a dependency today.

## Options
- Keep the display assumption and also run the UI tests under Xvfb on Linux CI. Windows and macOS runners already have a display. This changes CI.
- Add Monocle (or TestFX with a headless glass toolkit) so the same test runs on all three OS without a window. This is a new test dependency.
- Leave UI tests skipped on headless CI and rely on screenshots in the PR. That does not meet "every behaviour change ships with a test".

## Recommendation
E0-T05 proves one headless path on all three operating systems and writes the result in `docs/benchmarks/ui-tests.md`. The spike prefers the smallest change that makes `IdeWindowTest` actually run on `ubuntu-latest`: Xvfb if that is enough, Monocle if the suite must run inside the JVM with no display server. The spike does not edit `.github/workflows/ci.yml` until the owner accepts this ADR. CI workflow changes are ask-first.

Go means the existing `IdeWindowTest` methods execute (not skip) on all three CI operating systems. A no-go names which OS still skips and blocks E1 UI issues that claim a headless test, rather than merging tests that do not run.

## Consequences
New UI tests follow the harness this ADR accepts. They do not call `assumeFalse(isHeadless())` once the harness exists. Screenshot evidence in the PR is in addition to the test, not a substitute.

## Measured result (E0-T05, 2026-10-16)
`docs/benchmarks/ui-tests.md` ends with **go**. Xvfb (`xvfb-run -a`) makes all six `IdeWindowTest` methods run on `ubuntu-latest`; Windows and macOS already pass without it. Monocle was not required. This ADR stays **proposed**; `ci.yml` is not edited until acceptance.
